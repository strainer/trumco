import java.io.*;

public final class takeWavb {
/**
 * Wavfile gurgitator
 * 
 * Casual version - it only understands simple wav file formats
 * Opens and reads is 1 or 2 channel wavfile sequentialy
 * read ahead buffers and filereads automaticaly.
 *
 * Warning getChannel code is contentious...
 * Channels must be read in good sequence or 
 * unread channel data may get lost.
 *
 * This utility is written in private as part of a study 
 * portfolio in developement. 
 * 
 * neutron.soupmix@ntlworld.com */

public  int channels, rawPcmByts, totlFrames=0, bitsPerSample, Samprate; //stream details

private int hdChnkSz,fmt,dmt,wavFrmtTag; //metaMetas

byte[] loaded; 
byte[] bpass= new byte[0]; //possibly bugged tweak! -appears to work :)
private int samsGet, samsPer, framPerGet, FrmsBfd, rdIn, bytsRd, bytLft;
private boolean endian=false, headrd=true;

private int[] pwrCal= new int[6];
private int[] samsGvn;
private short[] retSam;
private short[][] samRng;
private byte samStep;
private int Fsize;
private int hdrskip=44;
private int lgiven=0;

public FileInputStream wavIn;
public File wavFile;                         //should these be private?

public takeWavb(String wvName, int samsPerGet, int smrt, int chls, int Bps, boolean endio)
{ samsGet=samsPerGet; samsPer=samsGet;
  headrd=false;
  channels=chls;
  //rawPcmByts, 
  //totlFrames, 
  bitsPerSample=Bps*8; 
  Samprate=smrt; 
  endian=endio;

  System.out.println("  Opening "+wvName);
  try
  { wavFile = new File(wvName);
    wavIn = new FileInputStream(wavFile);
    Fsize=wavIn.available();
    rawPcmByts=Fsize;
    wavIn.close();
  }

  catch(FileNotFoundException fnfe)
  { System.out.println("File Not Found " + wvName);
    return; }// end catch FileNotFoundException

  catch(IOException ioe)
  { System.out.println("IO Error: " + ioe);
    return; }// end catch IOException*/

  finally
  { } // end LoadArray
  
  initia();
  return;

}
  //bytbffa is sampls per modwork area

public takeWavb(String wvName, int samsPerGet)
{ samsGet=samsPerGet; samsPer=samsPerGet;           //(modwork area is modulated buffer space)
System.out.println("  Opening "+wvName);
try
{ wavFile = new File(wvName);
  wavIn = new FileInputStream(wavFile);
  Fsize=wavIn.available();
  
  byte[] hdrz = new byte[44];
  int hdrzlen = wavIn.read(hdrz);
  wavIn.close();
  byte hdrzi  = 12; //wavIn.skip(12);
  
  fmt                          // header should be fmt, fmt 4 bytes
  = ((hdrz[hdrzi++]&0x00ff)<<24) | ((hdrz[hdrzi++]&0x00ff)<<16)
  | ((hdrz[hdrzi++]&0x00ff)<<8) | ( (hdrz[hdrzi++]&0x00ff)); 

  if (fmt!=0x666D7420)
  { System.out.println("  fmt sig not encountered - Failure Likely...");
    System.out.println("  fmtsig val="+fmt+"\n");}

  hdChnkSz                    //size in bytes of a header chunk
  = ((hdrz[hdrzi++]&0x00ff) | ((hdrz[hdrzi++]&0x00ff)<<8)
  | ((hdrz[hdrzi++]&0x00ff)<<16) | ((hdrz[hdrzi++]&0x00ff)<<24)); 
  if (hdChnkSz!=16)
  { System.out.println("  Complex wav header - Failure Possible..."); }

  wavFrmtTag = (hdrz[hdrzi++]&0x00ff)|((hdrz[hdrzi++]&0x00ff)<<8 );

  channels   = (hdrz[hdrzi++]&0x00ff)|((hdrz[hdrzi++]&0x00ff)<<8 ); //channels

  Samprate 
  = ((hdrz[hdrzi++]&0x00ff) | ((hdrz[hdrzi++]&0x00ff)<<8)           //samprate
  | ((hdrz[hdrzi++]&0x00ff)<<16) | ((hdrz[hdrzi++]&0x00ff)<<24)); 
  
  hdrzi+=6;                          //skip byterate and blockalign
  bitsPerSample = (hdrz[hdrzi++]&0x00ff)|((hdrz[hdrzi++]&0x00ff)<<8 );

  //data chunk
  dmt                              // header should be dmt, 4 bytes
  = ((hdrz[hdrzi++]&0x00ff)<<24) | ((hdrz[hdrzi++]&0x00ff)<<16)
  | ((hdrz[hdrzi++]&0x00ff)<<8) | ( (hdrz[hdrzi++]&0x00ff)); 

  if (dmt!=0x64617461)
  { System.out.println("  No data signature found in wav - Abort! Abort! ");
    System.out.println("  dmtsig val="+dmt+"\n");}

  rawPcmByts                                        //size in bytes of pcm data
  = ((hdrz[hdrzi++]&0x00ff) | ((hdrz[hdrzi++]&0x00ff)<<8)
  | ((hdrz[hdrzi++]&0x00ff)<<16) | ((hdrz[hdrzi++]&0x00ff)<<24)); 

  if (Fsize!=(rawPcmByts+hdrskip))
  { System.out.println("  Unexpected Filesize :~/..."+Fsize+":"+(rawPcmByts+hdrskip) );
    if(Fsize<(rawPcmByts+hdrskip)){ rawPcmByts=Fsize-hdrskip; }    }
    //hdrzreading finished
    //hopefuly wav chunk headers overwith
  
  }//end io try

catch(FileNotFoundException fnfe)
{ System.out.println("File Not Found " + wvName);
  return; }// end catch FileNotFoundException

catch(IOException ioe)
{ System.out.println("IO Error: " + ioe);
  return; }// end catch IOException*/

finally
{ } // end LoadArray

initia();
return;
}//end constructor (open wav, get header details)

public void initia()
{ System.out.println("  SampleRate:"+Samprate); 
  totlFrames=rawPcmByts/(2*channels);

	if (channels==0){ System.out.println("  No Channels Arrrgh! Abort! Abort!"); channels=1; }
	if (channels==1){ System.out.println("  Mono input"); }
  if (channels==2){ System.out.println("  Stereo input"); }
	if (channels>=3){ System.out.println("  "+channels+" channels (weird, trying as mono)"  ); channels=1; }

  double smprmilli=1000d/(double)Samprate;
  long ThScnds=(long)((totlFrames)*smprmilli);
  System.out.println("  Samples "+(rawPcmByts)/(2*channels));
  System.out.println("  Trackime at "+Samprate+"Hz :"+ThScnds/1000+"."+(ThScnds%1000)/100+""+(ThScnds%100)/10+""+(ThScnds%10)+"\n");
  
	samStep=0;
  retSam=new short[samsGet];
  samRng=new short[channels][65536];
  samsGvn=new int[channels];
  FrmsBfd=0;
  bytsRd=0;

  rdIn=98304*channels; /*weird*/ //reading in 96k a time per channel= 48k 16bit samples
                                 //samRng is 64k 16 large
  } 

public void setHop(int hop)
{ setSkip( hop+lgiven ); }

public void setSkip(int skip)
{ samsGet=samsPer;
  if(retSam.length!=samsGet){ retSam = new short[samsGet]; };
  for(int i=0;i<channels;i++){ samsGvn[i]=skip; }
  FrmsBfd=skip;
  bytsRd=skip*2*(channels);
  rdIn=98304*channels;
  }  

public void setStep(byte set)
{ samStep = set; return; }

public int[] getPwr()
{ return pwrCal; }

public short[] getchn( int chnnel )
{ //System.out.println(" samsGvn[chnnel%2]:"+samsGvn[chnnel%2]
  //                  +" totlFrames:"+totlFrames+" samsGet:"+samsGet);
  
  if( (samsGvn[chnnel%2]+samsGet)>=(totlFrames) )
  { samsGet=totlFrames-samsGvn[chnnel%2];       //check if last get 
    if(samsGet<1){ //System.out.println("wavin buffer empty");
                   return new short[]{};      }
    retSam = new short[samsGet]; }

  if(FrmsBfd<(samsGvn[chnnel%2]+samsGet)) { movebf(); } //check if ringbuff needs moved
  
  //dervInt[chnnel]=0; mxSam[chnnel]=0;
  //pwrCal[2+chnnel]=0; pwrCal[4+chnnel]=0;

  if(chnnel<2)
  { for(int samsOut=0; samsOut<samsGet; samsOut++)
    { retSam[samsOut]=samRng[chnnel][(samsGvn[chnnel]++)%65536]; }
    }
  else
  { if(chnnel==2)
    { for(int samsOut=0; samsOut<samsGet; samsOut++)
      { retSam[samsOut]
        =(short)((samRng[0][(samsGvn[chnnel%2])%65536]-samRng[1][(samsGvn[chnnel%2]++)%65536])>>1); }
      }
    else
    { for(int samsOut=0; samsOut<samsGet; samsOut++)
      { retSam[samsOut]
        =(short)((samRng[0][(samsGvn[chnnel%2])%65536]+samRng[1][(samsGvn[chnnel%2]++)%65536])>>1); }
      }
    }
  
  lgiven=samsGvn[chnnel%2];
  return retSam; 
  }
  
public void movebf() //fetch next load of samples
{
	try

  { wavIn = new FileInputStream(wavFile);
	  wavIn.skip(hdrskip+bytsRd);
		if((bytsRd+rdIn)>rawPcmByts){ rdIn=rawPcmByts-bytsRd; } //stop rdin passing eof

    if(bpass.length!=rdIn){ bpass= new byte[rdIn]; } //when overflow, shrink bytepass
		bytsRd+= wavIn.read(bpass);
		loaded=bpass;
    wavIn.close();
		}
		catch(IOException ioe){ System.out.println("IO Error: " + ioe); return; }// end catch IOException
    finally{ } // end LoadArray
  
  if(channels==1)
  { for( int loadi=0; loadi<rdIn ; FrmsBfd++)
    { if(!endian)
      { samRng[0][FrmsBfd%65536]=(short)( (loaded[loadi++]&0x00ff)|((loaded[loadi++]<<8)&0xff00) ); }
      else
      { samRng[0][FrmsBfd%65536]=(short)( ((loaded[loadi++]<<8)&0xff00)|(loaded[loadi++]&0x00ff) ); }
      loadi+=samStep*2; } }
  else
  { for( int loadi=0; loadi<rdIn ; )
    { if(!endian)
      { samRng[0][FrmsBfd%65536]=(short)( (loaded[loadi++]&0x00ff)|((loaded[loadi++]<<8)&0xff00) );
        samRng[1][(FrmsBfd++)%65536]=(short)( (loaded[loadi++]&0x00ff)|((loaded[loadi++]<<8)&0xff00));}
      else
      { samRng[0][FrmsBfd%65536]=(short)( ((loaded[loadi++]<<8)&0xff00)|(loaded[loadi++]&0x00ff) );
        samRng[1][(FrmsBfd++)%65536]=(short)( ((loaded[loadi++]<<8)&0xff00)|(loaded[loadi++]&0x00ff));}
        
      loadi+=samStep*4; } }
	
  return;
  }//end eatWav

}//end class