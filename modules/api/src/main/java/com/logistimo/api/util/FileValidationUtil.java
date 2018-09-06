/*
 * Copyright © 2018 Logistimo.
 *
 * This file is part of Logistimo.
 *
 * Logistimo software is a mobile & web platform for supply chain management and remote temperature monitoring in
 * low-resource settings, made available under the terms of the GNU Affero General Public License (AGPL).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing a commercial license. To know more about
 * the commercial license, please contact us at opensource@logistimo.com
 */

package com.logistimo.api.util;

import com.logistimo.constants.Constants;
import com.logistimo.exception.ValidationException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.tika.Tika;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;

/**
 * Created by kumargaurav on 08/08/18.
 */
public class FileValidationUtil {

  private static final String[] allowedMediaExtenstion = {"jpeg", "jpg", "png", "gif"};
  private static final String[] allowedUploadFileExtenstion = {"csv", "xlsx", "xls", "xlsm"};

  private FileValidationUtil() {
  }

  public static void validateMediaFile(String fileExt) {
    if (!Arrays.asList(allowedMediaExtenstion).contains(fileExt)) {
      throw new ValidationException("ME001", fileExt);
    }
  }

  public static void validateUploadFile(String fileExt) {
    if (!Arrays.asList(allowedUploadFileExtenstion).contains(fileExt)) {
      throw new ValidationException("ME002", fileExt);
    }
  }

  /*
   * refer https://stackoverflow.com/questions/49633282/how-to-calculate-base64-encoded-string-length
   */
  public static void validateMediaFileSize(String data) {
    int fileLength = data.length();
    double size = Math.ceil(fileLength * 4 / 3.0);
    double sizeinMb = size / (1024 * 1024);
    if (sizeinMb > Constants.MEDIA_SIZE_IN_MB) {
      throw new ValidationException("ME003", new Object[]{null});
    }
  }

  public static void validateImageFile(byte[] content, Long checksum) {
    try {
      veirfyChecksum(content, checksum);
      String detectedContentType = new Tika().detect(content);
      if (!detectedContentType.startsWith("image")) {
        throw new ValidationException("Image content type does not match");
      }
      ImageIO.read(new ByteArrayInputStream(content));
    } catch (Exception e) {
      throw new ValidationException("ME006", new Object[]{null});
    }
  }

  private static void veirfyChecksum(byte[] content, Long checksum) {
    if (checksum != 0 && computeChecksum(content) != checksum) {
      throw new ValidationException("Image is not valid");
    }
  }

  public static long computeChecksum(byte[] content) {
    CRC32 crc = new CRC32();
    crc.update(content);
    return crc.getValue();
  }

  /**
   * Check if uploaded file is a valid excel file.
   *
   * @param content - file content
   */
  public static void validateExcelFile(byte[] content, long checksum) {
    try {
      veirfyChecksum(content, checksum);
      WorkbookFactory.create(
          new ByteArrayInputStream(content));
    } catch (IOException | InvalidFormatException e) {
      throw new ValidationException("ME007", new Object[]{null});
    }
  }
}
