<template>
  <v-container>
    <v-row>
      <v-col cols="12">
        <v-card>
          <v-card-title class="d-flex align-center">
            <span>{{ t('conversations.title') }}</span>
            <v-spacer></v-spacer>
            <v-btn 
              type="button"
              color="primary" 
              @click.stop.prevent="openExerciseChoice"
              :loading="creatingConversation"
              :disabled="creatingConversation"
            >
              <v-icon left>mdi-plus</v-icon>
              {{ t('conversations.new') }}
            </v-btn>
          </v-card-title>
          <v-card-text>
            <v-text-field
              v-model="search"
              :label="t('conversations.search')"
              prepend-inner-icon="mdi-magnify"
              clearable
              class="mb-4"
            ></v-text-field>

            <v-list v-if="conversations.length > 0">
              <ConversationItem
                v-for="conversation in filteredConversations"
                :key="conversation.id"
                :conversation="conversation"
                @delete="handleDelete"
                @select="handleSelect"
                @renamed="loadConversations"
              />
            </v-list>

            <v-alert v-else-if="!loading" type="info">
              {{ t('conversations.empty') }}
            </v-alert>

            <div v-if="loading" class="text-center mt-4">
              <v-progress-circular indeterminate color="primary" :aria-label="t('conversations.loading')" role="status"></v-progress-circular>
            </div>
          </v-card-text>
        </v-card>
        
        <!-- Snackbar for errors -->
        <v-snackbar
          v-model="showError"
          color="error"
          :timeout="5000"
          top
        >
          {{ errorMessage }}
          <template v-slot:action="{ attrs }">
            <v-btn
              text
              v-bind="attrs"
              @click="showError = false"
            >
              {{ t('conversations.close') }}
            </v-btn>
          </template>
        </v-snackbar>
      </v-col>
    </v-row>
    <ExerciseChoiceDialog
      v-model="choiceOpen"
      :loading="creatingConversation"
      @choose="createNewConversation"
      @cancel="choiceOpen = false"
    />
  </v-container>
</template>

<script>
import { apiService } from '../services/api';
import { authService } from '../services/auth';
import { exerciseTitle } from '../services/exerciseChoice';
import { t } from '../services/locale';
import ConversationItem from './ConversationItem.vue';
import ExerciseChoiceDialog from './ExerciseChoiceDialog.vue';

export default {
  name: 'ConversationList',
  components: {
    ConversationItem,
    ExerciseChoiceDialog
  },
  data() {
    return {
      conversations: [],
      loading: false,
      search: '',
      creatingConversation: false,
      choiceOpen: false,
      showError: false,
      errorMessage: ''
    };
  },
  computed: {
    filteredConversations() {
      if (!this.search) {
        return this.conversations;
      }
      const searchLower = this.search.toLowerCase();
      return this.conversations.filter(conv =>
        conv.title.toLowerCase().includes(searchLower)
      );
    }
  },
  async mounted() {
    await this.loadConversations();
  },
  methods: {
    async loadConversations() {
      const user = authService.getUser();
      if (!user || !user.id) {
        this.$router.push('/login');
        return;
      }

      this.loading = true;
      try {
        this.conversations = await apiService.getUserConversations(user.id);
      } catch (error) {
        console.error('Error loading conversations:', error);
      } finally {
        this.loading = false;
      }
    },

    openExerciseChoice(event) {
      if (event) {
        event.preventDefault();
        event.stopPropagation();
        event.stopImmediatePropagation();
      }
      this.choiceOpen = true;
    },

    async createNewConversation(activity) {
      const user = authService.getUser();
      if (!user || !user.id) {
        this.$router.push('/login');
        return;
      }

      this.creatingConversation = true;
      this.showError = false;
      this.errorMessage = '';

      try {
        console.log('Creating new conversation for user:', user.id, activity);
        const conversation = await apiService.createConversation(user.id, exerciseTitle(activity), activity);
        console.log('Conversation created successfully:', conversation);
        console.log('Response type:', typeof conversation);
        console.log('Conversation ID:', conversation?.id);
        
        // Validate response
        if (!conversation) {
          throw new Error(t('conversations.emptyResponse'));
        }
        
        if (!conversation.id) {
          console.error('Response without ID:', conversation);
          throw new Error(t('conversations.missingId'));
        }

        this.choiceOpen = false;

        // Navigate to chat screen with the created conversation ID
        const chatRoute = `/chat/${conversation.id}`;
        console.log('Navigating to:', chatRoute);
        await this.$router.push(chatRoute);
        console.log('Navigation completed');
      } catch (error) {
        console.error('Error creating new conversation:', error);
        console.error('Stack trace:', error.stack);
        const errorMsg = error.message || error.response?.data?.message || t('conversations.createError');
        this.errorMessage = errorMsg;
        this.showError = true;
        // Do not navigate on error - leave user on conversations page
      } finally {
        this.creatingConversation = false;
      }
    },

    handleSelect(conversationId) {
      this.$router.push(`/chat/${conversationId}`);
    },

    async handleDelete(conversationId) {
      const user = authService.getUser();
      if (!user || !user.id) {
        return;
      }

      try {
        await apiService.deleteConversation(conversationId, user.id);
        await this.loadConversations();
      } catch (error) {
        console.error('Error deleting conversation:', error);
      }
    }
  }
};
</script>

