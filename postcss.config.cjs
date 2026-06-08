module.exports = {
  plugins: [
    require('@fullhuman/postcss-purgecss')({
      content: [
        'src/main/resources/templates/**/*.html',
        'src/main/resources/static/js/**/*.js',
      ],
      safelist: {
        standard: ['active', 'show', 'collapse', 'collapsing', 'visually-hidden'],
        greedy: [/^d-/, /^navbar/, /^theme-/, /^kraft-/, /^is-/],
      },
      defaultExtractor: c => c.match(/[\w-/:]+(?<!:)/g) || [],
    }),
    require('cssnano')({ preset: 'default' }),
  ],
};
