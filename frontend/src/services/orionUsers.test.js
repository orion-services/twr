import test from 'node:test';
import assert from 'node:assert/strict';

import {
  unvalidatedEmailMessage,
  EMAIL_NOT_VALIDATED_MESSAGE,
  extractOrionErrorMessage,
  PASSWORD_RECOVERED_MESSAGE
} from './orionUsers.js';

test('asks the user to validate the email when the account is still unconfirmed', () => {
  assert.equal(
    unvalidatedEmailMessage({ email: 'ana@example.com', emailValid: false }),
    EMAIL_NOT_VALIDATED_MESSAGE
  );
});

test('allows login when the email is already validated', () => {
  assert.equal(unvalidatedEmailMessage({ emailValid: true }), null);
});

test('allows login when the payload does not say whether the email was validated', () => {
  assert.equal(unvalidatedEmailMessage({ email: 'ana@example.com' }), null);
  assert.equal(unvalidatedEmailMessage(null), null);
});

test('surfaces Orion validation messages when recovering a password fails', () => {
  assert.equal(
    extractOrionErrorMessage({
      response: { data: { message: 'Email not found' } }
    }),
    'Email not found'
  );
  assert.equal(
    extractOrionErrorMessage({
      response: { data: { violations: [{ message: 'Invalid email format' }] } }
    }),
    'Invalid email format'
  );
});

test('keeps a stable success copy after a new password is emailed', () => {
  assert.match(PASSWORD_RECOVERED_MESSAGE, /new password/i);
});
