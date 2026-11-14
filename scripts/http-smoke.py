#!/usr/bin/env python3
"""Run both packaged services against explicitly supplied disposable PostgreSQL databases.
Requires TEST_CORE_DATABASE_URL, TEST_TURF_DATABASE_URL, TEST_DATABASE_USERNAME,
TEST_DATABASE_PASSWORD and psql. Never point this script at an existing/production DB.
"""
import concurrent.futures
import datetime as dt
import json
import os
from pathlib import Path
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]
CORE, TURF = 'http://127.0.0.1:18080', 'http://127.0.0.1:18081'
SERVICE_KEY = 'smoke-only-service-key-at-least-thirty-two-characters'


def request(base, path, body=None, token=None, host=None, method=None, expected=200, key=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    if host:
        headers['X-Frontend-Host'] = host
    if key:
        headers['X-Service-Key'] = key
    req = urllib.request.Request(base + path, data=json.dumps(body).encode() if body is not None else None,
                                 headers=headers, method=method or ('POST' if body is not None else 'GET'))
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            status, payload = response.status, response.read()
    except urllib.error.HTTPError as ex:
        status, payload = ex.code, ex.read()
    assert status == expected, (path, status, payload.decode()[:500])
    value = json.loads(payload) if payload else {}
    return value.get('data', value)


def fixture(sql):
    url = os.environ['TEST_CORE_DATABASE_URL'].removeprefix('jdbc:')
    env = dict(os.environ, PGUSER=os.environ['TEST_DATABASE_USERNAME'],
               PGPASSWORD=os.environ.get('TEST_DATABASE_PASSWORD', ''))
    subprocess.run(['psql', url, '-v', 'ON_ERROR_STOP=1', '-c', sql], env=env, check=True,
                   stdout=subprocess.DEVNULL)


def login(email, password):
    return request(CORE, '/api/v1/auth/login', {'email': email, 'password': password})['accessToken']


def run():
    for name in ['TEST_CORE_DATABASE_URL', 'TEST_TURF_DATABASE_URL', 'TEST_DATABASE_USERNAME']:
        if not os.environ.get(name):
            raise SystemExit('Required disposable database setting: ' + name)
    tag = str(uuid.uuid4())
    password = 'Smoke-only-password-123!'
    email = 'super-' + tag + '@example.test'
    processes, logs = [], []
    try:
        for svc, port in [('core', 18080), ('turf', 18081)]:
            env = dict(os.environ, DATABASE_URL=os.environ['TEST_' + svc.upper() + '_DATABASE_URL'],
                       DATABASE_USERNAME=os.environ['TEST_DATABASE_USERNAME'],
                       DATABASE_PASSWORD=os.environ.get('TEST_DATABASE_PASSWORD', ''), PORT=str(port),
                       JWT_SECRET='smoke-only-jwt-secret-at-least-thirty-two-characters',
                       INTERNAL_SERVICE_KEY=SERVICE_KEY, CORE_URL=CORE, STORAGE_TYPE='s3',
                       BRANCH_ADMIN_EMAIL_DOMAIN='legacy.invalid', BRANCH_ADMIN_COMMON_PASSWORD=password,
                       SUPER_ADMIN_BOOTSTRAP_ENABLED='true', SUPER_ADMIN_FIRST_NAME='Smoke',
                       SUPER_ADMIN_LAST_NAME='Super', SUPER_ADMIN_EMAIL=email, SUPER_ADMIN_PASSWORD=password)
            jar = ROOT / f'tecgensoft-{svc}-service/target/tecgensoft-{svc}-service-0.0.1-SNAPSHOT.jar'
            log = tempfile.NamedTemporaryFile(prefix='tecgen-' + svc + '-', suffix='.log', delete=False)
            logs.append(log.name)
            java = str(Path(os.environ['JAVA_HOME']) / 'bin/java') if os.environ.get('JAVA_HOME') else 'java'
            processes.append(subprocess.Popen([java, '-jar', str(jar), '--aws.s3.region=eu-north-1',
                                                '--aws.s3.bucket-name=smoke-only'], env=env,
                                               stdout=log, stderr=subprocess.STDOUT))
        for _ in range(120):
            if any(p.poll() is not None for p in processes):
                raise RuntimeError('Service stopped; inspect ' + ', '.join(logs))
            try:
                super_token = login(email, password)
                break
            except (OSError, AssertionError):
                time.sleep(1)
        else:
            raise RuntimeError('Services did not start; inspect ' + ', '.join(logs))
        signup = {'firstName': 'Organization', 'lastName': 'Admin', 'email': 'org-' + tag + '@example.test',
                  'password': password}
        user = request(CORE, '/api/v1/auth/signup', signup, expected=201)
        # Signup responses use the project's existing response type.
        org_token = login(signup['email'], password)
        me = request(CORE, '/api/v1/users/me', token=org_token)
        admin_id = me['id']
        system = '/api/v1/system/saas'
        org = request(CORE, system + '/organizations', {'name': 'Smoke ' + tag, 'adminUserId': admin_id},
                      token=super_token, expected=201)
        org_id = org['id']
        plan = request(CORE, system + '/packages', {
            'code': 'SMOKE_' + tag, 'displayName': 'Smoke Basic', 'product': 'TURF', 'description': 'Disposable test',
            'currency': 'BDT', 'price': 1000, 'features': ['COURT_MANAGEMENT', 'BOOKING_MANAGEMENT', 'ONLINE_BOOKING',
            'PAYMENT_MANAGEMENT', 'REFUND_MANAGEMENT', 'VENUE_SETTINGS', 'WEBSITE_CONTENT', 'BASIC_REPORTS'],
            'limits': {'MAX_COURTS': 1, 'MAX_BRANCHES': 1}, 'active': True, 'availableForSale': True},
            token=super_token, expected=201)
        now = dt.datetime.now(dt.timezone.utc)
        request(CORE, system + f'/organizations/{org_id}/subscriptions', {
            'packageId': plan['id'], 'status': 'ACTIVE', 'billingPeriod': 'MONTHLY',
            'startsAt': (now - dt.timedelta(minutes=1)).isoformat(),
            'endsAt': (now + dt.timedelta(days=30)).isoformat(), 'agreedPrice': 900}, token=super_token, expected=201)
        domain = request(CORE, system + f'/organizations/{org_id}/domains', {
            'hostname': tag + '.example.test', 'product': 'TURF'}, token=super_token, expected=201)
        host = domain['hostname']
        # Test fixture only: .test cannot publish a real DNS TXT challenge.
        fixture("UPDATE tenant_domains SET verified_at=now() WHERE id='" + domain['id'] + "'")
        branch = request(CORE, system + f'/organizations/{org_id}/branches', {
            'name': 'Smoke branch ' + tag, 'address': 'Test address', 'phone': '0123'}, token=super_token, expected=201)
        private = '/api/v1/turf/branches/' + branch['id']
        public = '/api/v1/public/turf/branches/' + branch['id']
        for _ in range(60):
            try:
                request(TURF, public + '/courts', host=host)
                break
            except (OSError, AssertionError):
                time.sleep(1)
        settings = {'name': 'Test venue', 'phone': '0123', 'address': 'Test address', 'timezone': 'Asia/Dhaka',
                    'openingHour': 0, 'closingHour': 24, 'peakStartHour': 18, 'advancePercentage': 50,
                    'holdMinutes': 10, 'maxDurationHours': 4, 'bookingWindowDays': 30, 'cancellationCutoffHours': 0}
        request(TURF, private + '/settings', settings, org_token, host, 'POST', expected=201)
        court_request = {'name': 'Court', 'sport': 'FOOTBALL', 'surface': 'GRASS', 'indoor': False,
                         'regularPrice': 100, 'peakPrice': 200, 'players': 10, 'features': ['LIGHTS'], 'active': True}
        court = request(TURF, private + '/courts', court_request, org_token, host, expected=201)
        request(TURF, private + '/courts', dict(court_request, name='Excess'), org_token, host, expected=409)
        date = (now + dt.timedelta(days=2)).date().isoformat()
        slot = {'courtId': court['id'], 'date': date, 'startHour': 18, 'durationHours': 1}
        hold = request(TURF, public + '/holds', {'slot': slot}, host=host, expected=201)
        request(TURF, public + '/holds/' + hold['holdId'] + '/release', {'secret': 'bad'}, host=host,
                method='PATCH', expected=403)
        booking = request(TURF, public + '/bookings', {'holdId': hold['holdId'], 'secret': hold['secret'],
            'customerName': 'Customer', 'customerPhone': '0123', 'total': 1}, host=host, expected=201)
        assert booking['total'] == 200 and booking['requiredAdvance'] == 100
        payment = {'amount': 100, 'method': 'BKASH', 'kind': 'ADVANCE', 'reference': 'SMOKE-' + tag,
                   'idempotencyKey': tag, 'verified': True}
        ledger = request(TURF, private + '/bookings/' + booking['bookingId'] + '/payments', payment, org_token, host)
        retry = request(TURF, private + '/bookings/' + booking['bookingId'] + '/payments', payment, org_token, host)
        assert ledger['id'] == retry['id']
        details = request(TURF, private + '/bookings/' + booking['bookingId'], token=org_token, host=host)
        assert details['booking']['status'] == 'CONFIRMED' and 'ledger' not in details
        request(TURF, private + '/courts', token=super_token, host=host, expected=403)
        request(CORE, '/internal/v1/tenants/resolve', {'hostname': host}, key='invalid', expected=401)
        request(CORE, system + '/organizations', {'name': 'Denied', 'adminUserId': admin_id}, org_token, expected=403)
        request(TURF, private + '/settings', token='invalid', host=host, expected=401)
        request(TURF, private + '/reports?from=' + date + '&to=' + date, token=org_token, host=host)
        race_slot = dict(slot, startHour=20)
        def reserve(_):
            try:
                request(TURF, public + '/holds', {'slot': race_slot}, host=host, expected=201)
                return True
            except AssertionError as error:
                assert error.args[0][1] == 409
                return False
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
            assert sum(pool.map(reserve, range(4))) == 1
        print('HTTP smoke passed: both services, real authorization, pricing, checkout, confirmation, idempotency, denials, reports, concurrent holds.')
    finally:
        for process in processes:
            process.terminate()
        for process in processes:
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                process.kill()
        print('Service logs: ' + ', '.join(logs))


if __name__ == '__main__':
    run()
