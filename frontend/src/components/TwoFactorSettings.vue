<template>
  <v-container>
    <v-row>
      <v-col cols="12">
        <v-card>
          <v-card-title>{{ t('twoFactorSetup.title') }}</v-card-title>
          <v-card-text>
            <v-alert v-if="message" :type="messageType" class="mb-4">
              {{ message }}
            </v-alert>

            <!-- If 2FA is not enabled -->
            <div v-if="!twoFAEnabled">
              <p class="mb-4">{{ t('twoFactorSetup.intro') }}</p>
              <ol>
                <li>{{ t('twoFactorSetup.step1') }}</li>
                <li>{{ t('twoFactorSetup.step2') }}</li>
                <li>{{ t('twoFactorSetup.step3') }}</li>
              </ol>

              <v-form ref="qrForm" v-model="qrFormValid" class="mt-4">
                <v-text-field
                  v-model="qrEmail"
                  :label="t('field.email')"
                  required
                  prepend-inner-icon="mdi-email"
                  :rules="emailRules"
                ></v-text-field>

                <v-text-field
                  v-model="qrPassword"
                  :label="t('field.password')"
                  required
                  prepend-inner-icon="mdi-lock"
                  :type="showPassword ? 'text' : 'password'"
                  :rules="passwordRules"
                >
                  <template #append-inner>
                    <v-btn
                      icon
                      variant="text"
                      size="small"
                      :aria-label="showPassword ? t('password.hide') : t('password.show')"
                      @click="showPassword = !showPassword"
                    >
                      <v-icon aria-hidden="true">{{ showPassword ? 'mdi-eye' : 'mdi-eye-off' }}</v-icon>
                    </v-btn>
                  </template>
                </v-text-field>

                <v-btn
                  :disabled="!qrFormValid || loadingQR"
                  :loading="loadingQR"
                  color="primary"
                  @click="generateQRCode"
                >
                  {{ t('twoFactorSetup.generate') }}
                </v-btn>
              </v-form>

              <!-- Display QR Code -->
              <div v-if="qrCodeUrl" class="mt-4 text-center">
                <p class="mb-2">{{ t('twoFactorSetup.scan') }}</p>
                <img :src="qrCodeUrl" :alt="t('twoFactorSetup.qrAlt')" style="max-width: 300px;" />
                
                <v-form ref="validateForm" v-model="validateFormValid" class="mt-4">
                  <v-text-field
                    v-model="validationCode"
                    :label="t('field.code6')"
                    required
                    prepend-inner-icon="mdi-shield-lock"
                    maxlength="6"
                    :rules="codeRules"
                  ></v-text-field>

                  <v-btn
                    :disabled="!validateFormValid || loadingValidate"
                    :loading="loadingValidate"
                    color="success"
                    @click="validateCode"
                  >
                    {{ t('twoFactorSetup.validateEnable') }}
                  </v-btn>
                </v-form>
              </div>
            </div>

            <!-- If 2FA is enabled -->
            <div v-else>
              <v-alert type="success" class="mb-4">
                {{ t('twoFactorSetup.enabled') }}
              </v-alert>

              <v-form ref="settingsForm" v-model="settingsFormValid">
                <v-checkbox
                  v-model="require2FAForBasicLogin"
                  :label="t('twoFactorSetup.requireBasic')"
                ></v-checkbox>

                <v-checkbox
                  v-model="require2FAForSocialLogin"
                  :label="t('twoFactorSetup.requireSocial')"
                ></v-checkbox>

                <v-btn
                  :disabled="!settingsFormValid || loadingSettings"
                  :loading="loadingSettings"
                  color="primary"
                  @click="updateSettings"
                >
                  {{ t('twoFactorSetup.save') }}
                </v-btn>
              </v-form>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import { orionUsersService, extractOrionErrorMessage } from '../services/orionUsers';
import { authService } from '../services/auth';
import { t } from '../services/locale';

export default {
  name: 'TwoFactorSettings',
  data() {
    return {
      twoFAEnabled: false, // TODO: Check actual 2FA status
      qrFormValid: false,
      validateFormValid: false,
      settingsFormValid: true,
      qrEmail: '',
      qrPassword: '',
      showPassword: false,
      qrCodeUrl: null,
      validationCode: '',
      require2FAForBasicLogin: false,
      require2FAForSocialLogin: false,
      loadingQR: false,
      loadingValidate: false,
      loadingSettings: false,
      message: null,
      messageType: 'info',
      emailRules: [
        v => !!v || t('validation.emailRequired'),
        v => /.+@.+\..+/.test(v) || t('validation.emailInvalid')
      ],
      passwordRules: [
        v => !!v || t('validation.passwordRequired')
      ],
      codeRules: [
        v => !!v || t('validation.codeRequired'),
        v => (v && v.length === 6) || t('validation.codeSixDigits')
      ]
    };
  },
  methods: {
    async generateQRCode() {
      if (!this.$refs.qrForm.validate()) {
        return;
      }

      this.loadingQR = true;
      this.message = null;

      try {
        const blob = await orionUsersService.getQRCode(this.qrEmail, this.qrPassword);
        this.qrCodeUrl = URL.createObjectURL(blob);
        this.message = t('twoFactorSetup.qrSuccess');
        this.messageType = 'success';
      } catch (error) {
        console.error('Error generating QR code:', error);
        this.message = extractOrionErrorMessage(error) || t('twoFactorSetup.qrError');
        this.messageType = 'error';
      } finally {
        this.loadingQR = false;
      }
    },

    async validateCode() {
      if (!this.$refs.validateForm.validate()) {
        return;
      }

      this.loadingValidate = true;
      this.message = null;

      try {
        const response = await orionUsersService.validate2FA(
          this.qrEmail,
          this.qrPassword,
          this.validationCode
        );

        if (response.authentication && response.authentication.token) {
          // 2FA enabled successfully
          this.twoFAEnabled = true;
          this.message = t('twoFactorSetup.enabledSuccess');
          this.messageType = 'success';
          this.qrCodeUrl = null;
          this.validationCode = '';
        } else {
          this.message = t('twoFactor.invalid');
          this.messageType = 'error';
        }
      } catch (error) {
        console.error('Error validating code:', error);
        this.message = extractOrionErrorMessage(error) || t('twoFactor.invalid');
        this.messageType = 'error';
      } finally {
        this.loadingValidate = false;
      }
    },

    async updateSettings() {
      this.loadingSettings = true;
      this.message = null;

      try {
        const token = authService.getToken();
        const user = authService.getUser();
        const email = user?.email || this.qrEmail;

        await orionUsersService.update2FASettings(
          email,
          this.require2FAForBasicLogin,
          this.require2FAForSocialLogin,
          token
        );

        this.message = t('twoFactorSetup.saved');
        this.messageType = 'success';
      } catch (error) {
        console.error('Error updating settings:', error);
        this.message = extractOrionErrorMessage(error) || t('twoFactorSetup.saveError');
        this.messageType = 'error';
      } finally {
        this.loadingSettings = false;
      }
    }
  }
};
</script>

