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
 * Driven port (out) for downloading media attachments (e.g. WhatsApp voice notes).
 *
 * @author Rodrigo Prestes Machado
 */
public interface MediaDownloaderPort {

    /**
     * Downloads the binary content of a media item by its provider-assigned identifier.
     *
     * @param mediaId media identifier (e.g. the {@code audio.id} field from a webhook payload)
     * @return an {@link Optional} with the downloaded bytes, or empty if the download failed
     */
    Optional<byte[]> downloadMedia(String mediaId);
}
