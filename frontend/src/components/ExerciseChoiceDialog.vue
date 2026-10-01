<template>
  <v-dialog
    :model-value="modelValue"
    persistent
    max-width="480"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <v-card>
      <v-card-title class="d-flex align-start ga-2">
        <span class="text-wrap flex-grow-1">{{ copy.question }}</span>
        <v-btn
          icon
          variant="text"
          density="comfortable"
          :aria-label="copy.close"
          :disabled="loading"
          @click="$emit('cancel')"
        >
          <v-icon aria-hidden="true">mdi-close</v-icon>
        </v-btn>
      </v-card-title>
      <v-card-text>
        <v-btn
          block
          color="primary"
          class="mb-3"
          :loading="loading"
          :disabled="loading"
          @click="$emit('choose', 'CONNECTIVES')"
        >
          {{ copy.connectives }}
        </v-btn>
        <v-btn
          block
          color="primary"
          variant="outlined"
          :loading="loading"
          :disabled="loading"
          @click="$emit('choose', 'EXPANSION')"
        >
          {{ copy.expansion }}
        </v-btn>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<script>
import { exerciseChoiceCopy } from '../services/exerciseChoice';

export default {
  name: 'ExerciseChoiceDialog',
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'choose', 'cancel'],
  computed: {
    copy() {
      return exerciseChoiceCopy();
    }
  },
  watch: {
    modelValue: {
      immediate: true,
      handler(open) {
        if (open) {
          window.addEventListener('keydown', this.onEscape, true);
        } else {
          window.removeEventListener('keydown', this.onEscape, true);
        }
      }
    }
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.onEscape, true);
  },
  methods: {
    onEscape(event) {
      if (event.key !== 'Escape' || !this.modelValue || this.loading) {
        return;
      }
      event.preventDefault();
      event.stopPropagation();
      this.$emit('cancel');
    }
  }
};
</script>
