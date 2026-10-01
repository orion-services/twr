import axios from 'axios';

// Backend API Service (TWR)
// VITE_API_BASE_URL vazio (string '') significa "mesma origem" (produção: frontend
// servido pelo próprio Quarkus). Só cai no default localhost quando a variável não
// foi definida em nenhum momento do build (undefined), útil em dev sem .env.
const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL !== undefined
    ? import.meta.env.VITE_API_BASE_URL
    : 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

/**
 * Extrai o payload de uma linha SSE "data:...".
 * NÃO consome nenhum espaço após "data:" porque tokens do LLM começam com espaço
 * (ex.: " que", " você") e esse espaço É o separador de palavras.
 * Se o servidor adicionar um espaço de formatação, o HTML colapsa espaços extras.
 */
function sseDataPayload(line) {
  const s = line.replace(/\r$/, '');
  const m = s.match(/^\s*data:(.*)$/);
  return m ? m[1] : null;
}

// Interceptor para adicionar token JWT
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Interceptor para tratar erros de autenticação
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Token inválido ou expirado
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('user_data');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const apiService = {
  // Conversas
  async createConversation(userId, title, activity) {
    try {
      const response = await api.post(`/twr/users/${userId}/conversations`, { title, activity });
      if (!response.data || !response.data.id) {
        throw new Error('Resposta inválida do servidor: conversa criada sem ID');
      }
      return response.data;
    } catch (error) {
      console.error('Erro ao criar conversa:', error);
      // Re-throw com mensagem mais amigável
      if (error.response) {
        const message = error.response.data?.message || error.response.data?.error || 'Erro ao criar conversa';
        throw new Error(message);
      }
      throw error;
    }
  },

  async getUserConversations(userId) {
    const response = await api.get(`/twr/users/${userId}/conversations`);
    return response.data;
  },

  async getConversation(conversationId) {
    const response = await api.get(`/twr/conversations/${conversationId}`);
    return response.data;
  },

  async deleteConversation(conversationId, userId) {
    const response = await api.delete(`/twr/conversations/${conversationId}?userId=${userId}`);
    return response.data;
  },

  async updateConversationTitle(conversationId, title) {
    const response = await api.patch(`/twr/conversations/${conversationId}`, { title });
    return response.data;
  },

  async assignActivity(conversationId, activity) {
    const response = await api.patch(`/twr/conversations/${conversationId}/activity`, { activity });
    return response.data;
  },

  // Memória
  async getMemory(userId, conversationId) {
    try {
      const response = await api.get(`/twr/memory?userId=${userId}&conversationId=${conversationId}`);
      return response.data;
    } catch (error) {
      console.error('Erro ao carregar memória:', error);
      // Retornar null em caso de erro (conversa pode não ter histórico ainda)
      if (error.response && error.response.status === 404) {
        return null;
      }
      throw error;
    }
  },

  // Chatbot SSE (usando fetch com stream)
  async createChatbotStream(conversationId, prompt, onMessage, onError, onComplete) {
    return streamSse(`${API_BASE_URL}/twr/chatbot`, {
      conversationId: conversationId,
      prompt: prompt
    }, onMessage, onError, onComplete);
  },

  async startExerciseStream(conversationId, onMessage, onError, onComplete) {
    return streamSse(
      `${API_BASE_URL}/twr/conversations/${conversationId}/exercise`,
      {},
      onMessage,
      onError,
      onComplete
    );
  }
};

async function streamSse(url, body, onMessage, onError, onComplete) {
    const token = localStorage.getItem('jwt_token');
    if (!token) {
      onError(new Error('Token de autenticação não encontrado'));
      return;
    }

    let response;
    try {
      response = await fetch(url, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream'
        },
        body: JSON.stringify(body)
      });
    } catch (error) {
      onError(new Error(`Erro de conexão: ${error.message}`));
      return;
    }

    if (!response.ok) {
      let errorMessage = `Erro HTTP ${response.status}`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
      } catch (e) {
        // If unable to parse JSON, use default message
        const text = await response.text();
        if (text) {
          errorMessage = text;
        }
      }
      onError(new Error(errorMessage));
      return;
    }

    if (!response.body) {
      onError(new Error('Resposta sem corpo'));
      return;
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    // Per the SSE spec, ONE logical event's data can span MULTIPLE consecutive
    // "data:" lines — this happens whenever the underlying text contains a
    // literal newline (e.g. a paragraph break or list item emitted by the LLM).
    // Those lines must be rejoined with "\n" and delivered together as a single
    // message once the blank line that terminates the event is reached.
    //
    // The previous implementation dispatched every physical line as its own
    // independent message (via `onMessage`) and just concatenated the results
    // with no separator, which silently swallowed every embedded newline —
    // e.g. "...estudando.\n\nPercebi..." became "...estudando.Percebi..." in
    // the UI, breaking markdown paragraphs/lists.
    let eventDataLines = [];

    const flushEvent = () => {
      if (eventDataLines.length === 0) {
        // No "data:" line was accumulated for this event (e.g. consecutive
        // blank lines) — nothing to dispatch.
        return false;
      }
      const payload = eventDataLines.join('\n');
      eventDataLines = [];
      if (payload.trim() === '[DONE]') {
        onComplete();
        return true;
      }
      // NOTE: dispatch even if `payload` is an empty string — an event made
      // of a single empty "data:" line represents a real newline character
      // in the original text (e.g. the middle line of a "\n\n" paragraph
      // break split across "data:" lines) and must not be dropped.
      onMessage(payload);
      return false;
    };

    // Returns true if the caller should stop processing (stream finished).
    const processLine = (rawLine) => {
      const line = rawLine.replace(/\r$/, '');
      if (line === '') {
        // Blank line: terminates the current SSE event.
        return flushEvent();
      }
      const payload = sseDataPayload(line);
      if (payload !== null) {
        eventDataLines.push(payload);
        return false;
      }
      if (!line.startsWith(':') && !line.startsWith('event:') && !line.startsWith('id:') && !line.startsWith('retry:')) {
        // Defensive fallback for a non-strict SSE producer that omits the
        // "data:" prefix — still buffered/joined like a real data line so
        // embedded newlines across physical lines aren't lost.
        eventDataLines.push(line);
      }
      return false;
    };

    try {
      while (true) {
        const { done, value } = await reader.read();

        if (done) {
          // Processar buffer restante
          if (buffer.length > 0) {
            const lines = buffer.split('\n');
            for (const line of lines) {
              if (processLine(line)) {
                return;
              }
            }
          }
          flushEvent();
          onComplete();
          break;
        }

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (processLine(line)) {
            return;
          }
        }
      }
    } catch (error) {
      console.error('Erro ao processar stream:', error);
      onError(error);
    } finally {
      try {
        reader.releaseLock();
      } catch (e) {
        // Ignorar erro ao liberar lock
      }
    }
}

