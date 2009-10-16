package edance.userinterface.event;

import java.util.Vector;

import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.protocol.DataSource;

import edance.sceeneditor.MediaObjects;
import effects.CloneEffect;


public interface StreamListener {
	void addStream(/*CloneEffect*/DataSource cloneEffect, Format format, int position);
	void addVideo(CloneEffect cloneEffect, Format format, long ssrc, int position);
	void removeMedia(int position);
	void zOrderChanged(Vector<MediaObjects> mediaObjects);
	void addStream(MediaLocator locator, long ssrc, int position);
}
