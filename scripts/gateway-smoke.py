#!/usr/bin/env python3
"""Run Core, Turf and the gateway together and check tenant isolation through the gateway.
Build all three jars first. Requires TEST_CORE_DATABASE_URL, TEST_TURF_DATABASE_URL (empty,
disposable databases), TEST_DATABASE_USERNAME, TEST_DATABASE_PASSWORD and psql.
Never point this script at an existing/production DB.
"""
import concurrent.futures
import datetime as dt
import glob
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
CORE, TURF, GATEWAY = 'http://127.0.0.1:18080', 'http://127.0.0.1:18081', 'http://127.0.0.1:18000'
PLATFORM = 'https://admin.tecgensoft.com'
TAG = uuid.uuid4().hex[:8]
PASSWORD = 'Smoke-only-Password-123!'
SERVICE_KEY = 'smoke-only-service-key-at-least-thirty-two-characters'


class Reply:
    def __init__(self, status, headers, payload):
        self.status, self.headers = status, headers
        try:
            self.body = json.loads(payload) if payload else {}
        except ValueError:
            self.body = {'raw': payload.decode(errors='replace')}

    @property
    def data(self):
        return self.body.get('data', self.body)


def call(method, path, body=None, base=GATEWAY, raw=None, **headers):
    sent = {name.replace('_', '-'): value for name, value in headers.items()}
    data = raw
    if body is not None:
        data = json.dumps(body).encode()
        sent['Content-Type'] = 'application/json'
    request = urllib.request.Request(base + path, data=data, headers=sent, method=method)
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            return Reply(response.status, response.headers, response.read())
    except urllib.error.HTTPError as ex:
        return Reply(ex.code, ex.headers, ex.read())


def expect(status, reply, what):
    assert reply.status == status, f'{what}: expected {status}, got {reply.status} {reply.body}'
    print(f'  ok {status} {what}')
    return reply


def sql(database, statement):
    env = dict(os.environ, PGUSER=os.environ['TEST_DATABASE_USERNAME'],
               PGPASSWORD=os.environ.get('TEST_DATABASE_PASSWORD', ''))
    url = os.environ[database].removeprefix('jdbc:')
    return subprocess.run(['psql', url, '-v', 'ON_ERROR_STOP=1', '-Atc', statement], env=env, check=True,
                          capture_output=True, text=True).stdout.strip()


def start(name, pattern, env, logs):
    jar = [path for path in glob.glob(str(ROOT / pattern)) if not path.endswith('-plain.jar')][0]
    log = tempfile.NamedTemporaryFile(prefix=f'tecgen-{name}-', suffix='.log', delete=False)
    logs[name] = log.name
    return subprocess.Popen(['java', '-jar', jar], env=dict(os.environ, **env), stdout=log, stderr=subprocess.STDOUT)


def wait(process, log, attempt):
    for _ in range(120):
        assert process.poll() is None, 'Service stopped; inspect ' + log
        try:
            if attempt():
                return
        except OSError:
            pass
        time.sleep(1)
    raise AssertionError('Service did not start; inspect ' + log)


def login(email):
    # Straight to Core: the gateway's login limit is covered by its own tests.
    return expect(200, call('POST', '/api/v1/auth/login', {'email': email, 'password': PASSWORD}, base=CORE),
                  'login').data['accessToken']


def organization(name, token, package):
    now = dt.datetime.now(dt.timezone.utc)
    host, email = f'{name}-{TAG}.example.com', f'{name}-{TAG}@example.com'
    fields = {'name': f'{name} {TAG}', 'domain': host, 'adminEmail': email, 'adminPassword': PASSWORD,
              'packageId': package, 'startsAt': (now - dt.timedelta(days=1)).isoformat().replace('+00:00', 'Z'),
              'endsAt': (now + dt.timedelta(days=30)).isoformat().replace('+00:00', 'Z'), 'graceDays': '7',
              'amountPaid': '799'}
    boundary = uuid.uuid4().hex
    raw = ''.join(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'
                  for key, value in fields.items()) + f'--{boundary}--\r\n'
    expect(201, call('POST', '/api/v1/organizations', raw=raw.encode(), Origin=PLATFORM,
                     Authorization='Bearer ' + token,
                     Content_Type='multipart/form-data; boundary=' + boundary), f'create organization {name}')
    org = sql('TEST_CORE_DATABASE_URL', f"select organization_id from organization_domains where hostname='{host}'")
    return {'id': org, 'host': host, 'origin': 'https://' + host, 'token': login(email)}


def venue(org):
    admin = f"/api/v1/turf/organizations/{org['id']}"
    auth = {'Origin': PLATFORM, 'Authorization': 'Bearer ' + org['token']}
    expect(201, call('POST', admin + '/settings', {
        'name': org['host'], 'phone': '0123', 'address': 'Dhaka', 'timezone': 'Asia/Dhaka', 'openingHour': 0,
        'closingHour': 24, 'peakStartHour': 18, 'advancePercentage': 50, 'holdMinutes': 10, 'maxDurationHours': 4,
        'bookingWindowDays': 30, 'cancellationCutoffHours': 0}, **auth), 'create venue from the platform panel')
    org['court'] = expect(201, call('POST', admin + '/courts', {
        'name': 'Court', 'sport': 'FOOTBALL', 'surface': 'GRASS', 'indoor': False, 'regularPrice': 100,
        'peakPrice': 200, 'players': 10, 'features': [], 'active': True}, **auth), 'create court').data['id']
    org['admin'], org['auth'] = admin, auth


def scenario(processes, logs):
    print('onboarding')
    root = login(f'root-{TAG}@example.com')
    package = sql('TEST_CORE_DATABASE_URL', "select id from packages where code='TURF_BASIC'")
    a, b = organization('a', root, package), organization('b', root, package)
    venue(a)
    venue(b)
    public, date = '/api/v1/public/turf', (dt.date.today() + dt.timedelta(days=3)).isoformat()

    print('public site')
    for org in (a, b):
        site = expect(200, call('GET', '/api/v1/public/tenant', Origin=org['origin']), 'tenant').data
        assert site['organizationId'] == org['id'] and 'VENUE_SETTINGS' in site['features'], site
        assert not {'price', 'packageCode', 'subscriptionStatus'} & set(site), site
        assert expect(200, call('GET', public + '/venue', Origin=org['origin']), 'venue').data['name'] == org['host']
        expect(200, call('GET', public + '/content', Origin=org['origin']), 'content')
        courts = expect(200, call('GET', public + '/courts', Origin=org['origin']), 'courts').data
        assert [court['id'] for court in courts] == [org['court']], courts
    reply = call('GET', public + '/venue', Origin=a['origin'], X_Tenant_Host=b['host'])
    assert expect(200, reply, 'forged X-Tenant-Host loses to Origin').data['name'] == a['host']
    assert reply.headers.get_all('Access-Control-Allow-Origin') == [a['origin']]
    assert call('GET', public + '/venue', X_Tenant_Host=b['host']).data['name'] == b['host']

    print('booking')
    available = f"{public}/availability?courtId={a['court']}&date={date}"
    assert len(expect(200, call('GET', available, Origin=a['origin']), 'availability').data) == 24
    slot = {'courtId': a['court'], 'date': date, 'startHour': 18, 'durationHours': 1}
    hold = expect(201, call('POST', public + '/holds', {'slot': slot}, Origin=a['origin']), 'hold').data
    customer = {'holdId': hold['holdId'], 'secret': hold['secret'], 'customerName': 'C', 'customerPhone': '0123'}
    expect(404, call('GET', available, Origin=b['origin']), "B cannot read A's court")
    expect(404, call('POST', public + '/holds', {'slot': dict(slot, startHour=5)}, Origin=b['origin']),
           "B cannot hold A's court")
    expect(404, call('POST', public + '/bookings', customer, Origin=b['origin']), "B cannot check out A's hold")
    expect(404, call('GET', f"{public}/organizations/{b['id']}/venue", Origin=a['origin']),
           'organization path must match the site')
    booking = expect(201, call('POST', public + '/bookings', customer, Origin=a['origin']), 'checkout').data
    assert booking['status'] == 'PENDING_PAYMENT' and booking['requiredAdvance'] == 100, booking
    item = f"{public}/bookings/{booking['bookingId']}/payment-reference"
    reference = {'secret': hold['secret'], 'reference': 'TRX-' + TAG}
    expect(404, call('PATCH', item, reference, Origin=b['origin']), "B cannot touch A's booking")
    expect(200, call('PATCH', item, reference, Origin=a['origin']), 'payment reference')
    expect(400, call('POST', public + '/bookings', {'courtId': a['court'], 'bookingDate': date, 'startTime': '10:00',
                     'endTime': '11:00', 'customerPhone': '0123'}, Origin=a['origin']),
           'old direct booking is refused')

    print('administration')
    payment = {'amount': 100, 'method': 'BKASH', 'kind': 'ADVANCE', 'reference': 'TRX-' + TAG,
               'idempotencyKey': TAG, 'verified': True}
    target = f"/bookings/{booking['bookingId']}"
    expect(403, call('POST', a['admin'] + target + '/payments', payment, Origin=PLATFORM,
                     Authorization='Bearer ' + b['token']), "B's admin cannot use A's path")
    expect(404, call('POST', b['admin'] + target + '/payments', payment, **b['auth']),
           "B's admin cannot pay A's booking")
    expect(200, call('POST', a['admin'] + target + '/payments', payment, **a['auth']), 'admin verifies the advance')
    detail = expect(200, call('GET', a['admin'] + target, **a['auth']), 'booking detail').data
    assert detail['booking']['status'] == 'CONFIRMED', detail
    expect(401, call('GET', a['admin'] + '/bookings?from=' + date + '&to=' + date, Origin=PLATFORM), 'token required')
    expect(403, call('GET', a['admin'] + '/courts', Origin=a['origin'], Authorization='Bearer ' + a['token']),
           'management closed to site origins')

    print('edge')
    ask = {'Access_Control_Request_Method': 'POST'}
    expect(200, call('OPTIONS', public + '/holds', Origin=a['origin'], **ask), 'preflight')
    expect(403, call('OPTIONS', public + '/holds', Origin='https://evil.example.com', **ask), 'unknown preflight')
    expect(403, call('GET', public + '/venue', Origin='https://evil.example.com'), 'unknown origin')
    expect(400, call('GET', public + '/venue'), 'no tenant')
    expect(404, call('GET', public + '/venue', X_Tenant_Host='nobody.example.com'), 'unknown tenant')
    for path in ('/internal/v1/tenants/resolve', '/%69nternal/v1/tenants/resolve', '/v3/api-docs',
                 '/api/v1/admin/turf/bookings'):
        status = call('POST', path, {'hostname': a['host']}, X_Service_Key=SERVICE_KEY).status
        assert status in (400, 404), (path, status)
    print('  ok 4xx internal paths')

    print('concurrency')
    race = dict(slot, startHour=20)
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        statuses = list(pool.map(lambda _: call('POST', public + '/holds', {'slot': race}, Origin=a['origin']).status,
                                 range(6)))
    assert statuses.count(201) == 1 and statuses.count(409) == 5, statuses
    print('  ok one winner', statuses)

    print('suspension')
    sql('TEST_CORE_DATABASE_URL', f"update organizations set is_active=false where id='{b['id']}'")
    expect(403, call('GET', '/api/v1/public/tenant', Origin=b['origin']), 'tenant information closes at once')
    time.sleep(31)
    expect(403, call('GET', public + '/venue', Origin=b['origin']), 'public API closes within the cache time')
    expect(403, call('GET', b['admin'] + '/content', **b['auth']), 'management closes within the cache time')
    expect(200, call('GET', public + '/venue', Origin=a['origin']), 'other organizations are unaffected')

    print('core outage')
    processes['core'].terminate()
    processes['core'].wait(timeout=20)
    expect(503, call('GET', '/api/v1/organizations', Origin=PLATFORM), 'core down')
    expect(503, call('GET', public + '/venue', X_Tenant_Host='fresh.example.com'), 'tenant lookup down')


def run():
    for name in ('TEST_CORE_DATABASE_URL', 'TEST_TURF_DATABASE_URL', 'TEST_DATABASE_USERNAME'):
        if not os.environ.get(name):
            raise SystemExit('Required disposable database setting: ' + name)
    user, password = os.environ['TEST_DATABASE_USERNAME'], os.environ.get('TEST_DATABASE_PASSWORD', '')
    processes, logs = {}, {}
    try:
        processes['core'] = start('core', 'tecgensoft-core-service/target/backend-*.jar', {
            'PORT': '18080', 'SPRING_PROFILES_ACTIVE': 'local',
            'SPRING_DATASOURCE_URL': os.environ['TEST_CORE_DATABASE_URL'], 'SPRING_DATASOURCE_USERNAME': user,
            'SPRING_DATASOURCE_PASSWORD': password, 'SPRING_JPA_SHOW_SQL': 'false', 'STORAGE_TYPE': 's3',
            'JWT_SECRET': 'c21va2Utb25seS1qd3Qtc2VjcmV0LWF0LWxlYXN0LTMyLWNoYXJhY3RlcnM=',
            'INTERNAL_SERVICE_KEY': SERVICE_KEY, 'ORGANIZATION_ADMIN_EMAIL_DOMAIN': 'legacy.invalid',
            'ORGANIZATION_ADMIN_COMMON_PASSWORD': PASSWORD, 'SUPER_ADMIN_BOOTSTRAP_ENABLED': 'true',
            'SUPER_ADMIN_FIRST_NAME': 'Smoke', 'SUPER_ADMIN_LAST_NAME': 'Root',
            'SUPER_ADMIN_EMAIL': f'root-{TAG}@example.com', 'SUPER_ADMIN_PASSWORD': PASSWORD}, logs)
        wait(processes['core'], logs['core'], lambda: call('POST', '/api/v1/auth/login', {
            'email': f'root-{TAG}@example.com', 'password': PASSWORD}, base=CORE).status == 200)
        processes['turf'] = start('turf', 'tecgensoft-turf-service/target/tecgensoft-turf-service-*.jar', {
            'PORT': '18081', 'DATABASE_URL': os.environ['TEST_TURF_DATABASE_URL'], 'DATABASE_USERNAME': user,
            'DATABASE_PASSWORD': password or 'unused', 'CORE_URL': CORE, 'INTERNAL_SERVICE_KEY': SERVICE_KEY}, logs)
        processes['gateway'] = start('gateway', 'tecgensoft-gateway/target/tecgensoft-gateway-*.jar', {
            'PORT': '18000', 'MANAGEMENT_PORT': '18001', 'CORE_URL': CORE, 'TURF_URL': TURF,
            'INTERNAL_SERVICE_KEY': SERVICE_KEY, 'PLATFORM_ORIGINS': PLATFORM, 'RATE_LIMIT_ENABLED': 'false'}, logs)
        wait(processes['turf'], logs['turf'], lambda: call('GET', '/v3/api-docs', base=TURF).status == 200)
        wait(processes['gateway'], logs['gateway'], lambda: call('GET', '/nowhere').status == 404)
        assert call('GET', '/actuator/health', base='http://127.0.0.1:18001').status == 200
        scenario(processes, logs)
        print('Gateway smoke passed.')
    finally:
        for process in processes.values():
            process.terminate()
        for process in processes.values():
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                process.kill()
        print('Service logs: ' + ', '.join(logs.values()))


if __name__ == '__main__':
    run()
