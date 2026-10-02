/*
 * Copyright 2026 Rodrigo Prestes Machado
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.orion.twr.domain.port.out;

import java.util.Optional;

/**
 * Driven port (out) for transcribing audio content to text.
 *
 * @author Rodrigo Prestes Machado
 */
public interface SpeechToTextPort {

    /**
     * Transcribes audio data to text.
     *
     * @param audioData raw bytes of the audio file
     * @param mimeType  MIME type of the audio (e.g. {@code audio/ogg})
     * @return an {@link Optional} with the transcribed text, or empty if transcription failed
     */
    Optional<String> transcribe(byte[] audioData, String mimeType);
}
