import assert from 'node:assert/strict';
import test from 'node:test';
import { exerciseChoiceCopy, exerciseTitle, resolveExerciseLocale } from './exerciseChoice.js';

test('uses the first supported browser language', () => {
  assert.equal(resolveExerciseLocale(['fr-FR', 'es-MX', 'en']), 'es');
  assert.equal(resolveExerciseLocale(['pt-BR']), 'pt');
  assert.equal(resolveExerciseLocale(['en-US']), 'en');
  assert.equal(resolveExerciseLocale(['de']), 'en');
  assert.equal(resolveExerciseLocale([]), 'en');
  assert.equal(resolveExerciseLocale(['pt_BR']), 'pt');
  assert.equal(resolveExerciseLocale([], 'es-MX'), 'es');
  assert.equal(resolveExerciseLocale(undefined, 'pt-PT'), 'pt');
});

test('returns the modal copy and the conversation title in that language', () => {
  assert.equal(exerciseChoiceCopy(['pt-BR']).question, 'Que tipo de exercício você deseja fazer?');
  assert.equal(exerciseChoiceCopy(['es']).close, 'Cerrar');
  assert.equal(exerciseChoiceCopy(['en']).connectives, 'Connectives');
  assert.equal(exerciseTitle('EXPANSION', ['es-ES']), 'Expansión');
  assert.equal(exerciseTitle('CONNECTIVES', ['pt']), 'Conectivos');
});
