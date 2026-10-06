import assert from 'node:assert/strict';
import test from 'node:test';
import { catalogKeys, catalogLocales, formatDateTime, resolveLocale, t } from './locale.js';

test('keeps the original language tag and accepts underscores', () => {
  assert.deepEqual(resolveLocale(['pt_BR']), { locale: 'pt', tag: 'pt-BR' });
  assert.deepEqual(resolveLocale(['es-MX']), { locale: 'es', tag: 'es-MX' });
  assert.deepEqual(resolveLocale(['en-US']), { locale: 'en', tag: 'en-US' });
});

test('uses the fallback language when the preference list is empty', () => {
  assert.deepEqual(resolveLocale([], 'pt-BR'), { locale: 'pt', tag: 'pt-BR' });
  assert.deepEqual(resolveLocale(undefined, 'es'), { locale: 'es', tag: 'es' });
  assert.deepEqual(resolveLocale(['fr', 'de']), { locale: 'en', tag: 'en' });
});

test('prefers the first supported language', () => {
  assert.deepEqual(resolveLocale(['fr-FR', 'es-MX', 'en']), { locale: 'es', tag: 'es-MX' });
});

test('shares the same catalog keys in every language', () => {
  const expected = catalogKeys('en').sort();
  assert.ok(expected.length > 0);
  for (const code of catalogLocales()) {
    assert.deepEqual(catalogKeys(code).sort(), expected);
    for (const key of expected) {
      assert.notEqual(t(key, [code]), key);
    }
  }
});

test('formats dates with the requested language tag', () => {
  assert.equal(formatDateTime(''), '');
  assert.equal(formatDateTime('not-a-date'), '');
  assert.match(formatDateTime('2024-01-15T15:04:00Z', ['en-US']), /2024/);
  assert.match(formatDateTime('2024-01-15T15:04:00Z', ['pt-BR']), /2024/);
});
