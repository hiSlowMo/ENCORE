import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { describe, it } from 'node:test'
import ts from 'typescript'

// This standalone policy has no runtime imports. Use the already installed
// TypeScript compiler so the tests also work on Node 20 without a TS loader.
// npm run build separately performs the project's full type check.
const source = await readFile(new URL('../src/utils/credentialPolicy.ts', import.meta.url), 'utf8')
const compiled = ts.transpileModule(source, {
  fileName: 'credentialPolicy.ts',
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
  reportDiagnostics: true
})
assert.equal(compiled.diagnostics?.filter(row => row.category === ts.DiagnosticCategory.Error).length, 0)
const { validateDisplayName, validateUsername, validatePassword } = await import(
  'data:text/javascript;base64,' + Buffer.from(compiled.outputText).toString('base64')
)
const messages = Object.freeze({
  usernameRule: 'username-format', usernameReserved: 'username-reserved',
  passwordLength: 'password-length', passwordWhitespace: 'password-whitespace',
  passwordLettersNumbers: 'password-letter-number', passwordContainsUsername: 'password-username',
  passwordCommon: 'password-common', displayNameRule: 'nickname-length', displayNameControl: 'nickname-control'
})

describe('nickname ISO control parity with the backend policy', () => {
  // Character.isISOControl's explicit C0 + DEL + C1 ranges. Embedded values
  // keep trimming/length validation from hiding a missing control check.
  const controls = [...Array.from({ length: 32 }, (_, i) => i),
    ...Array.from({ length: 33 }, (_, i) => 0x7f + i)]
  for (const code of controls) {
    it(`rejects embedded U+${code.toString(16).padStart(4, '0')}`, () => {
      assert.equal(validateDisplayName(`A${String.fromCodePoint(code)}B`, messages), messages.displayNameControl)
    })
  }
  // Includes immediate neighbors, Chinese, separators and supplementary
  // characters. These are not ISO controls; this is not full Unicode parity.
  for (const code of [0x20, 0x21, 0x7e, 0xa0, 0xa1, 0x4e2d, 0x2028, 0x2029, 0x1f600, 0x1f680]) {
    it(`allows embedded non-control U+${code.toString(16)}`, () => {
      assert.equal(validateDisplayName(`A${String.fromCodePoint(code)}B`, messages), '')
    })
  }
  for (const value of ['', 'A', 'A'.repeat(33), '   ']) {
    it(`keeps nickname length rejection for ${JSON.stringify(value)}`, () => {
      assert.equal(validateDisplayName(value, messages), messages.displayNameRule)
    })
  }
  for (const value of ['AB', 'A'.repeat(32), '  Viewer  ', '观众']) {
    it(`keeps valid nickname ${JSON.stringify(value)}`, () => {
      assert.equal(validateDisplayName(value, messages), '')
    })
  }
})

describe('unrelated credential rules remain unchanged', () => {
  it('rejects reserved public signup names', () => {
    assert.equal(validateUsername('admin', messages), messages.usernameReserved)
  })
  it('retains the explicit staff-management reserved-name exception', () => {
    assert.equal(validateUsername('admin', messages, { allowReserved: true }), '')
  })
  it('retains username format validation even for staff', () => {
    assert.equal(validateUsername('1bad', messages, { allowReserved: true }), messages.usernameRule)
  })
  for (const [value, expected] of [
    ['Secure123', ''], ['Abc1234', messages.passwordLength],
    ['abc 12345', messages.passwordWhitespace], ['Abcdefgh', messages.passwordLettersNumbers],
    ['VALID_USER123', messages.passwordContainsUsername], ['pass1234', messages.passwordCommon]
  ]) {
    it(`keeps password result ${JSON.stringify(expected)} for ${JSON.stringify(value)}`, () => {
      assert.equal(validatePassword(value, 'valid_user', messages), expected)
    })
  }
})
