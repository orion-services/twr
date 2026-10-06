import { resolveLocale, t } from './locale.js';

export function resolveExerciseLocale(languages, fallback) {
  return resolveLocale(languages, fallback).locale;
}

function copy(languages) {
  const options = languages === undefined ? undefined : languages;
  return {
    question: t('exercise.question', options),
    connectives: t('exercise.connectives', options),
    expansion: t('exercise.expansion', options),
    close: t('exercise.close', options)
  };
}

export function exerciseChoiceCopy(languages) {
  return copy(languages);
}

export function exerciseTitle(activity, languages) {
  const text = copy(languages);
  return activity === 'EXPANSION' ? text.expansion : text.connectives;
}
