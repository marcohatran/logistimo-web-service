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

import com.logistimo.exception.ValidationException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FileValidationUtilTest {

  @Test
  public void checkValidationForCorrectPNG() throws IOException {
    validateImage("10x10-00ff007f.png");
  }

  @Test
  public void skipChecksum() throws IOException {
    TestFile fileContent = getClasspathFileContent("10x10-00ff007f.png");
    FileValidationUtil.validateImageFile(fileContent.content,
        0l);
  }

  private void validateImage(String fileName) throws IOException {
    TestFile fileContent = getClasspathFileContent(fileName);
    FileValidationUtil.validateImageFile(fileContent.content,
        fileContent.checksum);
  }

  private void validateExcel(String fileName) throws IOException {
    TestFile fileContent = getClasspathFileContent(fileName);
    FileValidationUtil.validateExcelFile(fileContent.content,
        fileContent.checksum);
  }

  @Test
  public void checkValidationForWrongPNG() throws IOException {
    try {
      validateImage("10x10-00ff007f-invalid.png");
      fail("Invalid png file not detected");
    } catch (ValidationException e) {
      assertEquals("ME006", e.getCode());
    }
  }


  @Test
  public void checkValidationForCorrectJPEG() throws IOException {
    validateImage("1x1.jpg");
  }

  @Test
  public void checkValidationForWrongJPEG() throws IOException {
    try {
      validateImage("1x1-invalid.jpg");
      fail("Invalid jpeg file not detected");
    } catch (ValidationException e) {
      assertEquals("ME006", e.getCode());
    }
  }

  @Test
  public void checkValidationForCorrectGIF() throws IOException {
    validateImage("1x1.gif");
  }

  @Test
  public void checkValidationForWrongGIF() throws IOException {
    try {
      validateImage("1x1-invalid.gif");
      fail("Invalid gif file not detected");
    } catch (ValidationException e) {
      assertEquals("ME006", e.getCode());
    }
  }

  @Test
  public void checkValidationForCorrectXLSX() throws IOException {
    validateExcel("test.xlsx");
  }

  @Test
  public void checkValidationForCorrectXLS() throws IOException {
    validateExcel("test.xls");
  }

  @Test
  public void checkValidationForCorrectXLSM() throws IOException {
    validateExcel("test.xlsm");
  }

  @Test
  public void checkValidationForWrongXLS() throws IOException {
    try {
      validateExcel("test-invalid.xls");
      fail("Invalid excel file not detected");
    } catch (ValidationException e) {
      assertEquals("ME007", e.getCode());
    }
  }


  private TestFile getClasspathFileContent(String fileName) throws IOException {
    return new TestFile(FileUtils.readFileToByteArray(
        new File(getClass().getClassLoader().getResource(fileName).getFile())));
  }
}

class TestFile {
  byte[] content;
  long checksum;

  TestFile(byte[] content) {
    this.content = content;
    this.checksum = FileValidationUtil.computeChecksum(content);
  }
}