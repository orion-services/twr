<template>
  <v-app>
    <v-app-bar color="primary">
      <v-app-bar-title>
        <span class="app-bar-brand">
          <img :src="appIcon" alt="" class="app-bar-logo" />
          <span>Orion TWR</span>
        </span>
      </v-app-bar-title>
      <v-spacer></v-spacer>
      <v-btn
        icon
        :aria-label="isDark ? 'Usar tema claro' : 'Usar tema escuro'"
        @click="toggleTheme"
      >
        <v-icon aria-hidden="true">{{ isDark ? 'mdi-weather-sunny' : 'mdi-weather-night' }}</v-icon>
      </v-btn>
      <v-btn v-if="!isAuthenticated" to="/register" icon aria-label="Registrar">
        <v-icon aria-hidden="true">mdi-account-plus</v-icon>
      </v-btn>
      <v-btn v-if="!isAuthenticated" to="/login" icon aria-label="Entrar">
        <v-icon aria-hidden="true">mdi-login</v-icon>
      </v-btn>
      <v-btn v-if="isAuthenticated" to="/conversations" icon aria-label="Conversas">
        <v-icon aria-hidden="true">mdi-message</v-icon>
      </v-btn>
      <v-btn v-if="isAuthenticated" to="/settings" icon aria-label="Configurações">
        <v-icon aria-hidden="true">mdi-cog</v-icon>
      </v-btn>
      <v-btn v-if="isAuthenticated" icon aria-label="Sair" @click="logout">
        <v-icon aria-hidden="true">mdi-logout</v-icon>
      </v-btn>
    </v-app-bar>
    <v-main class="fill-height" style="height: calc(100vh - 64px); overflow: hidden;">
      <router-view style="height: 100%;"></router-view>
    </v-main>
  </v-app>
</template>

<script>
import { useTheme } from 'vuetify';
import { useAuthStore } from './stores/auth';
import appIcon from './assets/icon1.png';

export default {
  name: 'App',
  setup() {
    const theme = useTheme();
    return { theme, appIcon };
  },
  computed: {
    isAuthenticated() {
      const authStore = useAuthStore();
      return authStore.isAuthenticated;
    },
    isDark() {
      return this.theme.global.current.value.dark;
    }
  },
  methods: {
    toggleTheme() {
      const next = this.isDark ? 'light' : 'dark';
      this.theme.global.name.value = next;
      localStorage.setItem('twr-theme', next);
    },
    logout() {
      const authStore = useAuthStore();
      authStore.logout();
      this.$router.push('/login');
    }
  }
};
</script>

<style scoped>
.app-bar-brand {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
}

.app-bar-logo {
  height: 40px;
  width: auto;
  display: block;
}
</style>

