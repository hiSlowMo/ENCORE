export const RESERVED_USERNAMES = ['admin', 'sysadmin', 'checker', 'root', 'system', 'encore']
export const COMMON_WEAK_PASSWORDS = [
  '12345678',
  '123456789',
  '11111111',
  'password',
  'password123',
  'qwerty123',
  'admin123',
  'encore123',
  'abc12345',
  'pass1234'
]

const usernamePattern = /^[a-z][a-z0-9_]{3,19}$/

export function normalizeUsername(value: string) {
  return value.trim()
}

export function isUsernameFormatValid(value: string) {
  return usernamePattern.test(normalizeUsername(value))
}

export function isReservedUsername(value: string) {
  return RESERVED_USERNAMES.includes(normalizeUsername(value))
}

export function validateUsername(value: string, messages: {
  usernameRule: string
  usernameReserved?: string
}, options: { allowReserved?: boolean } = {}) {
  const username = normalizeUsername(value)
  if (!isUsernameFormatValid(username)) return messages.usernameRule
  if (!options.allowReserved && messages.usernameReserved && isReservedUsername(username)) {
    return messages.usernameReserved
  }
  return ''
}

export function validatePassword(value: string, username: string, messages: {
  passwordLength: string
  passwordWhitespace: string
  passwordLettersNumbers: string
  passwordContainsUsername: string
  passwordCommon: string
}) {
  if (value.length < 8 || value.length > 64) return messages.passwordLength
  if (/\s/.test(value)) return messages.passwordWhitespace
  if (!/[A-Za-z]/.test(value) || !/\d/.test(value)) return messages.passwordLettersNumbers
  const normalizedUsername = normalizeUsername(username).toLowerCase()
  if (normalizedUsername && value.toLowerCase().includes(normalizedUsername)) {
    return messages.passwordContainsUsername
  }
  if (COMMON_WEAK_PASSWORDS.includes(value.toLowerCase())) return messages.passwordCommon
  return ''
}

export function validateDisplayName(value: string, messages: {
  displayNameRule: string
  displayNameControl: string
}) {
  const displayName = value.trim()
  if (displayName.length < 2 || displayName.length > 32) return messages.displayNameRule
  if (/[\u0000-\u001F\u007F-\u009F]/.test(displayName)) return messages.displayNameControl
  return ''
}
