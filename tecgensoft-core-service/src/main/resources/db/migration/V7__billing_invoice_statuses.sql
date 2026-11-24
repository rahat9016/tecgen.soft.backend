-- Billing invoice model adds new payment statuses (PAID/DUE/OVERDUE) and a
-- FREE_TRIAL method; unpaid (DUE/OVERDUE) invoices carry no method yet.

ALTER TABLE subscription_payments
    DROP CONSTRAINT subscription_payments_payment_method_check;

ALTER TABLE subscription_payments
    ALTER COLUMN payment_method DROP NOT NULL;

ALTER TABLE subscription_payments
    ADD CONSTRAINT subscription_payments_payment_method_check
    CHECK (payment_method IS NULL
           OR payment_method IN ('BKASH','NAGAD','SSL_COMMERZ','BANK','CASH','FREE_TRIAL'));

ALTER TABLE subscription_payments
    DROP CONSTRAINT subscription_payments_payment_status_check;

ALTER TABLE subscription_payments
    ADD CONSTRAINT subscription_payments_payment_status_check
    CHECK (payment_status IN ('PENDING','SUCCESS','FAILED','REFUNDED','PAID','DUE','OVERDUE'));
