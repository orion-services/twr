const EXERCISE_CHOICE = {
  pt: {
    question: 'Que tipo de exercício você deseja fazer?',
    connectives: 'Conectivos',
    expansion: 'Expansão',
    close: 'Fechar'
  },
  en: {
    question: 'What type of exercise do you want to do?',
    connectives: 'Connectives',
    expansion: 'Expansion',
    close: 'Close'
  },
  es: {
    question: '¿Qué tipo de ejercicio deseas hacer?',
    connectives: 'Conectivos',
    expansion: 'Expansión',
    close: 'Cerrar'
  }
};

export function resolveExerciseLocale(languages) {
  const list = Array.isArray(languages) && languages.length ? languages : ['en'];
  for (const lang of list) {
    const base = String(lang || '').toLowerCase().split('-')[0];
    if (base === 'pt' || base === 'es' || base === 'en') {
      return base;
    }
  }
  return 'en';
}

export function exerciseChoiceCopy(languages) {
  const source = languages ?? (typeof navigator !== 'undefined' ? navigator.languages : ['en']);
  return EXERCISE_CHOICE[resolveExerciseLocale(source)];
}

export function exerciseTitle(activity, languages) {
  const copy = exerciseChoiceCopy(languages);
  return activity === 'EXPANSION' ? copy.expansion : copy.connectives;
}
