module.exports = {
  extends: ['@dachshaus/eslint-config'],
  parserOptions: {
    project: './tsconfig.lint.json',
    tsconfigRootDir: __dirname,
  },
  rules: {
    '@typescript-eslint/no-explicit-any': 'off',
  },
};
