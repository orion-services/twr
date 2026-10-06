import axios from 'axios';
import { t } from './locale.js';

/**
 * Extracts a human-readable error message from an Orion Users error response.
 *
 * Orion Users does NOT always return `{ message: "..." }` on errors — validation
 * failures (e.g. weak password, malformed email) come back as either:
 *   { violations: [{ message: "..." }, ...] }
 * or:
 *   { title: "Constraint Violation", violations: [{ field, message }] }
 * A naive `error.response?.data?.message` lookup misses both shapes and silently
 * falls back to a generic message, hiding the real (often actionable) reason.
 */
export function extractOrionErrorMessage(error) {
  const data = error?.response?.data;
  if (!data) {
    return null;
  }
  if (Array.isArray(data.violations) && data.violations.length > 0) {
    return data.violations.map((v) => v.message).filter(Boolean).join('; ');
  }
  return data.message || data.title || null;
}

/** Shown when password login succeeds but the account email is still unconfirmed. */
export function emailNotValidatedMessage(languages) {
  return t('auth.emailNotValidated', languages);
}

/** Shown after Orion Users generates a new password and emails it. */
export function passwordRecoveredMessage(languages) {
  return t('auth.passwordRecovered', languages);
}

/**
 * Password login is refused until the account email is confirmed.
 * Google sign-in is left alone: the provider has already verified the address.
 *
 * @param {object | null | undefined} user user payload from Orion Users
 * @param {string[] | string | undefined} languages browser languages, when the caller wants a specific copy
 * @returns {string | null} the message to show, or null when login may proceed
 */
export function unvalidatedEmailMessage(user, languages) {
  if (user && user.emailValid === false) {
    return emailNotValidatedMessage(languages);
  }
  return null;
}

// Orion Users API Service
// Só cai no default localhost quando a variável não foi definida em nenhum momento
// do build (undefined) — em produção, VITE_ORION_USERS_URL deve apontar para a URL
// pública real do serviço Orion Users (não há fallback seguro para produção).
const orionUsersUrlFromEnv = import.meta.env?.VITE_ORION_USERS_URL;
const ORION_USERS_URL =
  orionUsersUrlFromEnv !== undefined
    ? orionUsersUrlFromEnv
    : 'http://localhost:8080';

const orionUsersApi = axios.create({
  baseURL: ORION_USERS_URL,
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded'
  }
});

export const orionUsersService = {
  // Registrar usuário
  async createUser(name, email, password) {
    const formData = new URLSearchParams();
    formData.append('name', name);
    formData.append('email', email);
    formData.append('password', password);
    
    const response = await orionUsersApi.post('/users/create', formData);
    return response.data;
  },

  // Registrar e autenticar em uma única requisição
  async createAndAuthenticate(name, email, password) {
    const formData = new URLSearchParams();
    formData.append('name', name);
    formData.append('email', email);
    formData.append('password', password);
    
    const response = await orionUsersApi.post('/users/createAuthenticate', formData);
    return response.data;
  },

  // Login
  async login(email, password) {
    const formData = new URLSearchParams();
    formData.append('email', email);
    formData.append('password', password);
    
    const response = await orionUsersApi.post('/users/login', formData);
    return response.data;
  },

  // Recover password: Orion generates a new password and emails it (HTTP 204).
  async recoverPassword(email) {
    const formData = new URLSearchParams();
    formData.append('email', email);

    const response = await orionUsersApi.post('/users/recoverPassword', formData);
    return response.data;
  },

  // Login com Google (Social Authentication)
  async loginWithGoogle(idToken) {
    const formData = new URLSearchParams();
    formData.append('idToken', idToken);
    
    const response = await orionUsersApi.post('/users/login/google', formData);
    return response.data;
  },

  // Login com 2FA
  async loginWith2FA(email, code) {
    const formData = new URLSearchParams();
    formData.append('email', email);
    formData.append('code', code);
    
    const response = await orionUsersApi.post('/users/login/2fa', formData);
    return response.data;
  },

  // Obter QR code para 2FA
  async getQRCode(email, password) {
    const formData = new URLSearchParams();
    formData.append('email', email);
    formData.append('password', password);
    
    const response = await orionUsersApi.post('/users/google/2FAuth/qrCode', formData, {
      responseType: 'blob'
    });
    return response.data;
  },

  // Validar código 2FA
  async validate2FA(email, password, code) {
    const formData = new URLSearchParams();
    formData.append('email', email);
    formData.append('password', password);
    formData.append('code', code);
    
    const response = await orionUsersApi.post('/users/google/2FAuth/validate', formData);
    return response.data;
  },

  // Atualizar configurações 2FA
  async update2FASettings(email, require2FAForBasicLogin, require2FAForSocialLogin, token) {
    const formData = new URLSearchParams();
    formData.append('email', email);
    if (require2FAForBasicLogin !== undefined) {
      formData.append('require2FAForBasicLogin', require2FAForBasicLogin);
    }
    if (require2FAForSocialLogin !== undefined) {
      formData.append('require2FAForSocialLogin', require2FAForSocialLogin);
    }
    
    const response = await orionUsersApi.post('/users/2fa/settings', formData, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    return response.data;
  }
};

