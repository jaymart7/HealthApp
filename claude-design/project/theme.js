// Shared source of truth for HealthTrack tokens + health math.
// Session 1 keeps Onboarding.dc.html's inline copy (verified working); from Session 2
// onward, new DCs should import from here instead of re-typing hex/formula literals.
export const THEMES = {
  light: {
    primary: '#37693D', onPrimary: '#FFFFFF', primaryContainer: '#B8F1B9', onPrimaryContainer: '#1E5027',
    secondary: '#516350', onSecondary: '#FFFFFF', secondaryContainer: '#D4E8D0', onSecondaryContainer: '#3A4B3A',
    tertiary: '#39656C', onTertiary: '#FFFFFF', tertiaryContainer: '#BDEAF3', onTertiaryContainer: '#1F4D54',
    error: '#BA1A1A', onError: '#FFFFFF', errorContainer: '#FFDAD6', onErrorContainer: '#93000A',
    background: '#F7FBF2', onBackground: '#181D18', surface: '#F7FBF2', onSurface: '#181D18',
    surfaceVariant: '#DEE5D9', onSurfaceVariant: '#424940', surfaceDim: '#D7DBD3', surfaceBright: '#F7FBF2',
    surfaceContainerLowest: '#FFFFFF', surfaceContainerLow: '#F1F5EC', surfaceContainer: '#EBEFE6',
    surfaceContainerHigh: '#E5E9E1', surfaceContainerHighest: '#E0E4DB',
    outline: '#727970', outlineVariant: '#C1C9BE', scrim: '#000000',
    inverseSurface: '#2D322C', inverseOnSurface: '#EEF2E9', inversePrimary: '#9DD49E',
  },
  dark: {
    primary: '#9DD49E', onPrimary: '#023913', primaryContainer: '#1E5027', onPrimaryContainer: '#B8F1B9',
    secondary: '#B8CCB5', onSecondary: '#243424', secondaryContainer: '#3A4B3A', onSecondaryContainer: '#D4E8D0',
    tertiary: '#A1CED6', onTertiary: '#00363D', tertiaryContainer: '#1F4D54', onTertiaryContainer: '#BDEAF3',
    error: '#FFB4AB', onError: '#690005', errorContainer: '#93000A', onErrorContainer: '#FFDAD6',
    background: '#101510', onBackground: '#E0E4DB', surface: '#101510', onSurface: '#E0E4DB',
    surfaceVariant: '#424940', onSurfaceVariant: '#C1C9BE', surfaceDim: '#101510', surfaceBright: '#363A35',
    surfaceContainerLowest: '#0B0F0B', surfaceContainerLow: '#181D18', surfaceContainer: '#1C211C',
    surfaceContainerHigh: '#272B26', surfaceContainerHighest: '#313630',
    outline: '#8C9389', outlineVariant: '#424940', scrim: '#000000',
    inverseSurface: '#E0E4DB', inverseOnSurface: '#2D322C', inversePrimary: '#37693D',
  },
};

export const ACT_MULT = { sedentary: 1.2, light: 1.375, moderate: 1.55, very: 1.725 };
export const GOAL_ADJ = { lose: -500, maintain: 0, build: 300 };
export const CM_PER_IN = 2.54;
export const KG_PER_LB = 0.453592;

// Mifflin-St Jeor -> TDEE -> goal-adjusted calories, clamped to the safety floor.
export function calcDailyTargets({ sex, age, heightCm, weightKg, activity, goal }) {
  const isMale = sex === 'male';
  const bmr = isMale
    ? 10 * weightKg + 6.25 * heightCm - 5 * age + 5
    : 10 * weightKg + 6.25 * heightCm - 5 * age - 161;
  const tdee = bmr * (ACT_MULT[activity] || 1.2);
  const floor = isMale ? 1500 : 1200;
  let calories = Math.round(tdee + (GOAL_ADJ[goal] || 0));
  if (calories < floor) calories = floor;
  return {
    calories,
    protein: Math.round((calories * 0.30) / 4),
    carbs: Math.round((calories * 0.40) / 4),
    fat: Math.round((calories * 0.30) / 9),
    floor,
  };
}
