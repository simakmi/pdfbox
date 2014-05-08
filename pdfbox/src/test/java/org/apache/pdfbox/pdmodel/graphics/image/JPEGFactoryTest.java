/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.pdmodel.graphics.image;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import junit.framework.TestCase;
import static junit.framework.TestCase.assertTrue;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import static org.apache.pdfbox.pdmodel.graphics.image.ValidateXImage.colorCount;
import static org.apache.pdfbox.pdmodel.graphics.image.ValidateXImage.validate;

/**
 * Unit tests for JPEGFactory
 *
 * @author Tilman Hausherr
 */
public class JPEGFactoryTest extends TestCase
{
    private final File testResultsDir = new File("target/test-output/graphics");

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        testResultsDir.mkdirs();
    }

    /**
     * Tests JPEGFactory#createFromStream(PDDocument document, InputStream
     * stream) with color JPEG file
     */
    public void testCreateFromStream() throws IOException
    {
        PDDocument document = new PDDocument();
        InputStream stream = JPEGFactoryTest.class.getResourceAsStream("jpeg.jpg");
        PDImageXObject ximage = JPEGFactory.createFromStream(document, stream);
        validate(ximage, 8, 344, 287, "jpg", PDDeviceRGB.INSTANCE.getName());
        assertEquals(PDDeviceRGB.INSTANCE, ximage.getColorSpace());
        document.close();
    }

    /**
     * Tests JPEGFactory#createFromStream(PDDocument document, InputStream
     * stream) with gray JPEG file
     */
    public void testCreateFromStream256() throws IOException
    {
        PDDocument document = new PDDocument();
        InputStream stream = JPEGFactoryTest.class.getResourceAsStream("jpeg256.jpg");
        PDImageXObject ximage = JPEGFactory.createFromStream(document, stream);
        validate(ximage, 8, 344, 287, "jpg", PDDeviceGray.INSTANCE.getName());
        assertEquals(PDDeviceGray.INSTANCE, ximage.getColorSpace());
        document.close();
    }

    /**
     * Tests RGB JPEGFactory#createFromImage(PDDocument document, BufferedImage
     * image) with color JPEG image
     */
    public void testCreateFromImageRGB() throws IOException
    {
        PDDocument document = new PDDocument();
        BufferedImage image = ImageIO.read(JPEGFactoryTest.class.getResourceAsStream("jpeg.jpg"));
        assertEquals(3, image.getColorModel().getNumComponents());
        PDImageXObject ximage = JPEGFactory.createFromImage(document, image);
        validate(ximage, 8, 344, 287, "jpg", PDDeviceRGB.INSTANCE.getName());
        assertEquals(PDDeviceRGB.INSTANCE, ximage.getColorSpace());
        document.close();
    }

    /**
     * Tests RGB JPEGFactory#createFromImage(PDDocument document, BufferedImage
     * image) with gray JPEG image
     */
    public void testCreateFromImage256() throws IOException
    {
        PDDocument document = new PDDocument();
        BufferedImage image = ImageIO.read(JPEGFactoryTest.class.getResourceAsStream("jpeg256.jpg"));
        assertEquals(1, image.getColorModel().getNumComponents());
        PDImageXObject ximage = JPEGFactory.createFromImage(document, image);
        validate(ximage, 8, 344, 287, "jpg", PDDeviceGray.INSTANCE.getName());
        assertEquals(PDDeviceGray.INSTANCE, ximage.getColorSpace());
        document.close();
    }

    /**
     * Tests ARGB JPEGFactory#createFromImage(PDDocument document, BufferedImage
     * image)
     */
    public void testCreateFromImageINT_ARGB() throws IOException
    {
        PDDocument document = new PDDocument();
        BufferedImage image = ImageIO.read(JPEGFactoryTest.class.getResourceAsStream("jpeg.jpg"));

        // create an ARGB image
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage argbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics ag = argbImage.getGraphics();
        ag.drawImage(image, 0, 0, null);
        ag.dispose();

        for (int x = 0; x < argbImage.getWidth(); ++x)
        {
            for (int y = 0; y < argbImage.getHeight(); ++y)
            {
                argbImage.setRGB(x, y, (argbImage.getRGB(x, y) & 0xFFFFFF) | ((y / 10 * 10) << 24));
            }
        }

        PDImageXObject ximage = JPEGFactory.createFromImage(document, argbImage);
        validate(ximage, 8, width, height, "jpg", PDDeviceRGB.INSTANCE.getName());
        assertNotNull(ximage.getSoftMask());
        validate(ximage.getSoftMask(), 8, width, height, "jpg", PDDeviceGray.INSTANCE.getName());
        assertTrue(colorCount(ximage.getSoftMask().getImage()) > image.getHeight() / 10);

        // This part isn't really needed because this test doesn't break
        // if the mask has the wrong colorspace (PDFBOX-2057), but it is still useful
        // if something goes wrong in the future and we want to have a PDF to open.
        PDPage page = new PDPage();
        document.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(document, page, true, false);
        contentStream.drawXObject(ximage, 150, 300, ximage.getWidth(), ximage.getHeight());
        contentStream.drawXObject(ximage, 200, 350, ximage.getWidth(), ximage.getHeight());
        contentStream.close();
        File pdfFile = new File(testResultsDir, "jpeg-intargb.pdf");
        document.save(pdfFile);
        document.close();
        document = PDDocument.loadNonSeq(pdfFile, null);
        document.close();
    }

    /**
     * Tests ARGB JPEGFactory#createFromImage(PDDocument document, BufferedImage
     * image)
     */
    public void testCreateFromImage4BYTE_ABGR() throws IOException
    {
        PDDocument document = new PDDocument();
        BufferedImage image = ImageIO.read(JPEGFactoryTest.class.getResourceAsStream("jpeg.jpg"));

        // create an ARGB image
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage argbImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics ag = argbImage.getGraphics();
        ag.drawImage(image, 0, 0, null);
        ag.dispose();

        for (int x = 0; x < argbImage.getWidth(); ++x)
        {
            for (int y = 0; y < argbImage.getHeight(); ++y)
            {
                argbImage.setRGB(x, y, (argbImage.getRGB(x, y) & 0xFFFFFF) | ((y / 10 * 10) << 24));
            }
        }

        PDImageXObject ximage = JPEGFactory.createFromImage(document, argbImage);
        validate(ximage, 8, width, height, "jpg", PDDeviceRGB.INSTANCE.getName());
        assertNotNull(ximage.getSoftMask());
        validate(ximage.getSoftMask(), 8, width, height, "jpg", PDDeviceGray.INSTANCE.getName());
        assertTrue(colorCount(ximage.getSoftMask().getImage()) > image.getHeight() / 10);

        // This part isn't really needed because this test doesn't break
        // if the mask has the wrong colorspace (PDFBOX-2057), but it is still useful
        // if something goes wrong in the future and we want to have a PDF to open.
        PDPage page = new PDPage();
        document.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(document, page, true, false);
        contentStream.drawXObject(ximage, 150, 300, ximage.getWidth(), ximage.getHeight());
        contentStream.drawXObject(ximage, 200, 350, ximage.getWidth(), ximage.getHeight());
        contentStream.close();
        File pdfFile = new File(testResultsDir, "jpeg-4bargb.pdf");
        document.save(pdfFile);
        document.close();
        document = PDDocument.loadNonSeq(pdfFile, null);
        document.close();
    }
}
