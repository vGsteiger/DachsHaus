module.exports = {
  extends: ['next/core-web-vitals', '@dachshaus/eslint-config'],
  parserOptions: {
    project: './tsconfig.json',
    tsconfigRootDir: __dirname,
  },
};
