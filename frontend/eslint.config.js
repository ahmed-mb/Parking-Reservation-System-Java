// ESLint flat config for the React 18 + Vite frontend.
//
// This is the modern (ESLint 9+) configuration format. Each entry in the
// exported array is an "override" applied to files matching its `files`
// glob; later entries win over earlier ones. The config is intentionally
// pragmatic: it catches real bugs (unused imports, accidental ==, missing
// hook deps) without forcing a stylistic argument.
//
// Run locally with:  npm run lint
// CI runs:           npm run lint     (which is `eslint . --max-warnings 0`)

import js from "@eslint/js";
import react from "eslint-plugin-react";
import reactHooks from "eslint-plugin-react-hooks";
import globals from "globals";

export default [
  // 1. Skip generated / vendored output entirely.
  {
    ignores: [
      "dist/**",
      "coverage/**",
      "node_modules/**",
      "public/**",
    ],
  },

  // 2. Apply ESLint's recommended JS rules to every JS/JSX file.
  js.configs.recommended,

  // 3. React + React Hooks rules.
  {
    files: ["**/*.{js,jsx}"],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "module",
      parserOptions: {
        ecmaFeatures: { jsx: true },
      },
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },
    plugins: {
      react,
      "react-hooks": reactHooks,
    },
    settings: {
      react: { version: "detect" },
    },
    rules: {
      // Inherit the recommended sets from the plugins.
      ...react.configs.recommended.rules,
      ...reactHooks.configs.recommended.rules,

      // We use the modern JSX transform; importing React in every file
      // is no longer necessary, so disable the legacy rule.
      "react/react-in-jsx-scope": "off",
      // PropTypes is not in use; this project relies on call-site testing.
      "react/prop-types": "off",
      // `useEffect`'s dependency array is the most common React bug source;
      // promote it from "warn" to "error" so it actually fails the build.
      "react-hooks/exhaustive-deps": "error",
      // Catch the small mistakes that compound: dead variables and == vs ===.
      "no-unused-vars": ["error", {
        argsIgnorePattern: "^_",
        varsIgnorePattern: "^_",
      }],
      eqeqeq: ["error", "always"],
    },
  },

  // 4. Test files: relax a couple of rules that are noisy in vitest.
  {
    files: ["src/test/**/*.{js,jsx}", "**/*.{test,spec}.{js,jsx}"],
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node,
        // vitest's globals (describe, it, expect, beforeEach, etc.) are
        // injected when the test runs, so allow them in lint context too.
        describe: "readonly",
        it: "readonly",
        test: "readonly",
        expect: "readonly",
        beforeEach: "readonly",
        afterEach: "readonly",
        beforeAll: "readonly",
        afterAll: "readonly",
        vi: "readonly",
      },
    },
    rules: {
      "no-unused-vars": "off",
    },
  },
];
