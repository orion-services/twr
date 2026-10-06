<template>
  <v-list-item @click="$emit('select', conversation.id)">
    <v-text-field
      v-if="editing"
      ref="titleField"
      v-model="newTitle"
      density="compact"
      variant="underlined"
      hide-details
      single-line
      autofocus
      :aria-label="t('conversations.titleLabel')"
      :disabled="renaming"
      @click.stop
      @keydown.enter.prevent="submitRename"
      @keydown.esc.prevent="cancelRename"
      @blur="submitRename"
    />
    <v-list-item-title v-else>{{ conversation.title }}</v-list-item-title>
    <v-list-item-subtitle>
      {{ t('conversations.createdAt', { date: formatDate(conversation.startedAt) }) }}
      <span v-if="conversation.lastActivity">
        {{ t('conversations.lastActivity', { date: formatDate(conversation.lastActivity) }) }}
      </span>
    </v-list-item-subtitle>

    <template #append>
      <div @click.stop>
        <v-btn
          icon
          variant="text"
          :aria-label="editing ? t('conversations.saveTitle') : t('conversations.rename')"
          :loading="renaming"
          @mousedown.prevent
          @click="editing ? submitRename() : openRename()"
        >
          <v-icon>{{ editing ? 'mdi-check' : 'mdi-pencil' }}</v-icon>
        </v-btn>
        <v-btn
          icon
          variant="text"
          :aria-label="t('conversations.delete')"
          @click="confirmDelete"
        >
          <v-icon>mdi-delete</v-icon>
        </v-btn>
      </div>
    </template>
  </v-list-item>
  <v-divider></v-divider>
</template>

<script>
import { apiService } from '../services/api';
import { formatDateTime, t } from '../services/locale';

export default {
  name: 'ConversationItem',
  props: {
    conversation: {
      type: Object,
      required: true
    }
  },
  emits: ['select', 'delete', 'renamed'],
  data() {
    return {
      editing: false,
      newTitle: '',
      renaming: false
    };
  },
  methods: {
    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      if (Number.isNaN(date.getTime())) return '';
      return formatDateTime(date);
    },

    openRename() {
      this.newTitle = this.conversation.title || '';
      this.editing = true;
      this.$nextTick(() => {
        this.$refs.titleField?.focus?.();
      });
    },

    cancelRename() {
      this.editing = false;
      this.newTitle = this.conversation.title || '';
    },

    async submitRename() {
      if (!this.editing || this.renaming) {
        return;
      }
      const title = (this.newTitle || '').trim();
      if (!title || title === (this.conversation.title || '').trim()) {
        this.cancelRename();
        return;
      }
      this.renaming = true;
      try {
        await apiService.updateConversationTitle(this.conversation.id, title);
        this.editing = false;
        this.$emit('renamed');
      } catch (e) {
        console.error('Error renaming conversation:', e);
        alert(e.response?.data?.message || e.message || t('conversations.renameError'));
      } finally {
        this.renaming = false;
      }
    },

    confirmDelete() {
      if (confirm(t('conversations.confirmDelete'))) {
        this.$emit('delete', this.conversation.id);
      }
    }
  }
};
</script>

