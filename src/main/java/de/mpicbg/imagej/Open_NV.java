package de.mpicbg.imagej;

import java.io.*;
import java.util.*;
import java.awt.*;

import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.io.*;
import ij.measure.*;

// Jim Hull, The University of Washington Chemical Engineering
// Authored 01/04
// Modified on 04/2007 by Philippe Carl, Life science project engineer Veeco Instruments GmbH
// E-mail: pcarl@veeco.de

public class Open_NV implements PlugIn
{
    ArrayList<HashMap<String, Object>> headerProperties = null;

    FileInfo nFileInfo;
    FileOpener nFileOpener;
    ImagePlus nImage;
    ImageProcessor nProc;
    Calibration nCal;
    private int bufferCount=0;

    String buf = "";

    BufferedReader fileInputStream;
    File file;

    private double[]   nVProps   = new double[3];
    private double[][] buffProps = new double[5][8];
    private int colonIndex, unitSkip;

    public void run(String arg)
    {
        //IJ.log("Entering Plugin");
        if (arg.equals(""))
        {
            OpenDialog od = new OpenDialog("Open NV...", "C:\\Philippe\\registration", "");
            String fileName = od.getFileName();

            if (fileName==null)
                return;

            String directory = od.getDirectory();
            IJ.showStatus("Opening: " + directory + fileName);
            file = new File(directory + fileName);
        }
        else
            file = new File(arg);

        // Open the STP file
        openNV(file);

    }	//end run


    public void openNV(File fileName)
    {
        headerProperties = new ArrayList<HashMap<String, Object>>();

        //IJ.log("Entering openStp");

        //int buffers=0;
        double[]   fileProps   = new double[10];
        double[][] bufferProps = new double[10][5];

        //Create the file input stream
        try
        {
            fileInputStream = new BufferedReader(new FileReader(fileName));
            IJ.showStatus("Opening Buffered Reader");
        }
        catch (FileNotFoundException exception)
        {
            IJ.showStatus("Buffered Reader Exception");
        }

        //Read the Header
        readHeader(fileInputStream);

        //Close the input stream
        try
        {
            fileInputStream.close();
            IJ.showStatus("Closing Buffered Reader");
        }
        catch (IOException exception)
        {
            IJ.showStatus("Buffered Reader Exception");
        }

        //Set up the images
        /*for(int i = 0; i < bufferCount; i++)
        {

            //Set Parameters and open each buffer
            nFileInfo = new FileInfo();
            nFileInfo.fileType = nFileInfo.GRAY16_SIGNED;
            nFileInfo.fileName = fileName.toString();
            nFileInfo.width  = (int) nVProps[1];
            nFileInfo.height = (int) nVProps[1];
            nFileInfo.offset = (int)buffProps[0][i];
            nFileInfo.intelByteOrder = true;

            //Open the Image
            nFileOpener = new FileOpener(nFileInfo);
            nImage = nFileOpener.open(true);

        }	//end for
        */

        int counter = 0;
        for (HashMap<String, Object> header : headerProperties) {
            counter++;
            System.out.println("Loading " + fileName + " [" + counter + "]");
            //Set Parameters and open each buffer
            nFileInfo = new FileInfo();
            int bytesPerPixel = (int)header.get("bytesperpixel");
            switch (bytesPerPixel) {
                case 1:
                    nFileInfo.fileType = FileInfo.GRAY8;
                    break;
                case 4:
                    nFileInfo.fileType = FileInfo.GRAY32_FLOAT;
                    break;
                case 2:
                default:
                    nFileInfo.fileType = FileInfo.GRAY16_UNSIGNED;
                    break;
            }
            nFileInfo.fileName = fileName.toString();
            nFileInfo.width  = (int)header.get("width");
            if (header.containsKey("height")) {
                nFileInfo.height = (int) header.get("height");
            } else if (header.containsKey("data_length")) {
                nFileInfo.height =  (int)header.get("data_length") / nFileInfo.width;
            }
            if (header.containsKey("depth")) {
                nFileInfo.nImages = (int)header.get("depth");
            } else {
                nFileInfo.nImages = (int)header.get("data_length") / nFileInfo.width / nFileInfo.height;
            }

            nFileInfo.offset = (int)header.get("data_offset");
            nFileInfo.intelByteOrder = true;

            //Open the Image
            nFileOpener = new FileOpener(nFileInfo);
            nImage = nFileOpener.open(true);

            if (header.containsKey("title")) {
                nImage.setTitle((String)header.get("title"));
            }
        }

    }	//end openNV

    public void readHeader(BufferedReader in)
    {
        //Scan through header for file info
        while(!buf.endsWith("File list end"))
        {
            //IJ.log("buffer count " + new Integer(bufferCount).toString());
            try
            {
                buf = new String(in.readLine());
            }
            catch (IOException exception)
            {
                IJ.showStatus("IO Exception");
            }

            buf=buf.substring(1);
            //IJ.log(buf);
            colonIndex=buf.indexOf(":");
            //IJ.log(new Integer(colonIndex).toString());
            unitSkip=buf.length()-2;
            //IJ.log(new Integer(unitSkip).toString());

            //Get the buffer info
            //Get the header size
            if(buf.startsWith("Data length:")) // && bufferCount == 0)
            {
                addImageProperty("data_length", new Integer(buf.substring(colonIndex + 1).trim()));

                //IJ.log(new Double(buf.substring(colonIndex + 1).trim()).toString());
                nVProps[0] = new Double(buf.substring(colonIndex + 1).trim()).doubleValue();
                IJ.log("Data Length " + (int) nVProps[0]);
                //bufferCount++;
                continue;
            } else

            if (buf.startsWith("Samps/line: ")) {
                String[] temp = buf.substring(colonIndex + 1).trim().split(" ");
                addImageProperty("width", new Integer(temp[0]));
                //if (temp.length > 1) {
                //    addImageProperty("depth", new Integer(temp[1]));
                //}





            } else


            if (buf.startsWith("Bytes/pixel: ")) {
                addImageProperty("bytesperpixel", new Integer(buf.substring(colonIndex + 1).trim()));
            } else

            if (buf.startsWith("Number of lines: ")) {
                addImageProperty("height", new Integer(buf.substring(colonIndex + 1).trim()));
            } else

            if (buf.startsWith("@2:Image Data: ")) {
                String[] temp = buf.split(":");
                addImageProperty("title", temp[temp.length - 1].trim());
            } else



            //Get the number of pixels
            if (buf.startsWith("Lines:")) // && bufferCount == 0)
            {
                addImageProperty("height", new Integer(buf.substring(colonIndex + 1).trim()));
                nVProps[1] = new Double(buf.substring(colonIndex + 1).trim()).doubleValue();
                IJ.log("Lines " + (int) nVProps[1]);
                continue;
            } else



            if (buf.startsWith("Scan size:")) // && bufferCount == 0)
            {
                addImageProperty("scan_size", new Double(buf.substring(colonIndex + 1, unitSkip).trim()));
                nVProps[2] = new Double(buf.substring(colonIndex + 1, unitSkip).trim()).doubleValue();
                IJ.log("Scan Size " + nVProps[2]);
                continue;
            } else

                //Get the image size and offset
            if (buf.startsWith("Data offset:"))
            {
                appendImage();
                addImageProperty("data_offset", new Integer(buf.substring(colonIndex + 1).trim()));

                buffProps[0][bufferCount] = new Integer(buf.substring(colonIndex + 1).trim()).intValue();
                bufferCount += 1;
                IJ.log("Data Offset " + new Integer((int) buffProps[0][bufferCount - 1]).toString());
                continue;
            } else
            if (buf.startsWith("Data length:") && bufferCount>0)
            {
                addImageProperty("data_length", new Integer(buf.substring(colonIndex + 1).trim()));
                buffProps[1][bufferCount - 1] = new Integer(buf.substring(colonIndex + 1).trim()).intValue();
                IJ.log("Data Sizez " +      new Integer((int) buffProps[1][bufferCount - 1]).toString());
                //bufferCount++;
                continue;
            } else {
                //IJ.log(buf);
            }

        }	//end for

    }	//end readHeader

    private void addImageProperty(String key, Object value) {
        if (headerProperties.size() != 0) {
            HashMap<String, Object> header = headerProperties.get(headerProperties.size() - 1);
            header.put(key, value);
            System.out.println("  " + key + " = " + value);
        }
    }

    private void appendImage() {
        System.out.println("NEW IMAGE");
        headerProperties.add(new HashMap<String, Object>());
    }

    public static void main(String[] args) {
        new ImageJ();
        new Open_NV().openNV(new File("D:/structure/data/mt_naked_.0_00000.spm"));
    }


}	//end class