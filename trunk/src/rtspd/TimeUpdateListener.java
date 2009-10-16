/**
 * 
 */
package rtspd;

/**
 * @author Anja Le Blanc
 * interface to receive updates from the replay of sessions
 *
 */
public interface TimeUpdateListener {
	void updateTime(long time);
}
