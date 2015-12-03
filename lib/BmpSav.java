
import java.io.*;

public class BmpSav
{ public static void main(String[] args) 
	{ System.out.println("BmpSav is not standalone"); }
	

public BmpSav (int[] BmapSrc, int imWdth, int imHght, String outFileName) //Bitmap output constructer method
{ 
	try  //Write BmapSrc to File(outFileName)
	{ FileOutputStream fout = new FileOutputStream(new File(outFileName));
    System.out.println("  Saving " + outFileName);

		fout.write(66); fout.write(77); // bfType 19778 must always be set to 'BM' to declare that this is a .bmp-file.

		byte pado = (byte)(4-((imWdth*3)%4));
		if (pado ==4 ) { pado=0; }
		int bmpsize=54+(imWdth+pado)*imHght*3; //4 bfSize ?? specifies the size of the file in bytes. 
		fout.write(bmpsize); fout.write(bmpsize>>8); fout.write(bmpsize>>16); fout.write(bmpsize>>24); 
    
		//4 bfReserved1 0 must always be set to zero.
		fout.write(0); fout.write(0); fout.write(0); fout.write(0);
 
    int bfOffBits=54; //4 bfOffBits 1078 specifies the offset from the beginning of the file to the bitmap data. 
		fout.write(bfOffBits); fout.write(bfOffBits>>8); fout.write(bfOffBits>>16); fout.write(bfOffBits>>24); 

    int biSize=40; //4 biSize 40 specifies the size of the BITMAPINFOHEADER structure, in bytes. 
		fout.write(biSize); fout.write(biSize>>8); fout.write(biSize>>16); fout.write(biSize>>24); 

		int biWidth=imWdth; //4 biWidth - specifies the width of the image, in pixels. 
		fout.write(biWidth); fout.write(biWidth>>8); fout.write(biWidth>>16); fout.write(biWidth>>24); 

		int biHeight=imHght; //4 biHeight 100 specifies the height of the image, in pixels. 
		fout.write(biHeight); fout.write(biHeight>>8); fout.write(biHeight>>16); fout.write(biHeight>>24); 

		short biPlanes=1; //2 biPlanes 1 specifies the number of planes of the target device, must be set to zero. 
		fout.write(biPlanes); fout.write(biPlanes>>8); 

		short biBitCount=24; //2 biBitCount 8 specifies the number of bits per pixel.
		fout.write(biBitCount); fout.write(biBitCount>>8); 

		int biCompression=0; //4 biCompression 0 Specifies the type of compression, usually set to zero (no compression). 
		fout.write(biCompression); fout.write(biCompression>>8); fout.write(biCompression>>16); fout.write(biCompression>>24); 

		int biSizeC=0; //4 biSizeImage 0 size of the image data, in bytes. If no compression, it is valid to set this to zero. 
		fout.write(biSizeC); fout.write(biSizeC>>8); fout.write(biSizeC>>16); fout.write(biSizeC>>24); //biCompress

		fout.write(0); fout.write(0); fout.write(0); fout.write(0); //four more zero ints
		fout.write(0); fout.write(0); fout.write(0); fout.write(0);
		fout.write(0); fout.write(0); fout.write(0); fout.write(0);
		fout.write(0); fout.write(0); fout.write(0); fout.write(0);

    //System.out.println(" imHght"+imHght+" imWdth"+imWdth);

//start of quad array:
    byte[] roobees =new byte[(imWdth)*3];
    for (int row=(imHght-1)*imWdth; row >= 0 ; row=row-imWdth )
    {	//System.out.print(" roob"+row+"eee ");
    
		for (int col =0 ; col <imWdth ; col++ )
	  { roobees[col*3]= (byte)(BmapSrc[row+col]>>>0 ); //System.out.print(sceneDpixels[i]);
      roobees[col*3+1]=(byte)(BmapSrc[row+col]>>>8 );
		  roobees[col*3+2]=(byte)(BmapSrc[row+col]>>>16 );
			//fout.write(0);
      } //end for

    fout.write(roobees);  //write line at time instead pixels save delivery trips
    
		for (byte pug=pado ; pug!=0 ; pug-- ) { fout.write(0); } //Add Pad
									
		} //finished lines

    fout.close();
		} //end try fout

  catch(IOException ioe) { System.out.println("IO Error: " + ioe); }// end catch IOException
	} //end constructer method

} //end class
