package net.sf.fmj.media;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.PushBufferStream;

import net.sf.fmj.utility.LoggerSingleton;

import com.lti.utils.synchronization.CloseableThread;
import com.lti.utils.synchronization.ProducerConsumerQueue;
import com.lti.utils.synchronization.SynchronizedObjectHolder;

/**
 * Helper class to do BufferTransferHandler notifications in a background thread.
 * @author Ken Larson
 *
 */
public class AsyncBufferTransferHandlerNotifier
{
    private static final Logger logger = LoggerSingleton.logger;

    private final PushBufferStream stream;
    private final SynchronizedObjectHolder<BufferTransferHandler> transferHandlerHolder = new SynchronizedObjectHolder<BufferTransferHandler>();

    public AsyncBufferTransferHandlerNotifier(PushBufferStream stream)
    {
        super();
        this.stream = stream;
    }

    public void setTransferHandler(BufferTransferHandler transferHandler)
    {	transferHandlerHolder.setObject(transferHandler);
    }

    public void notifyTransferHandlerSync()
    {
        final BufferTransferHandler handler = (BufferTransferHandler) transferHandlerHolder.getObject();
        if (handler != null)
            handler.transferData(stream);
    }

    public void notifyTransferHandlerAsync() throws InterruptedException
    {
        if (notifyTransferHandlerThread == null)
        {	notifyTransferHandlerThread = new NotifyTransferHandlerThread("NotifyTransferHandlerThread for " + stream);
            notifyTransferHandlerThread.start();
        }

        notifyTransferHandlerThread.notifyTransferHandlerAsync();
    }

    public void dispose()
    {
        if (notifyTransferHandlerThread != null)
        {
            notifyTransferHandlerThread.close();
            try
            {
                notifyTransferHandlerThread.waitUntilClosed();
            } catch (InterruptedException e)
            {
                logger.log(Level.WARNING, "" + e, e);
            }
            finally
            {
                notifyTransferHandlerThread = null;
            }
        }
    }

    public void disposeAsync()
    {
        if (notifyTransferHandlerThread != null)
        {
            notifyTransferHandlerThread.close();
            notifyTransferHandlerThread = null;
        }
    }

    private NotifyTransferHandlerThread notifyTransferHandlerThread;

    // doing the transfer notifications in a different thread keeps the
    // capture thread from being tied up.  Seems to avoid some deadlocks when
    // JMF is ahead in the classpath as well.
    class NotifyTransferHandlerThread extends CloseableThread
    {

        public NotifyTransferHandlerThread(String threadName)
        {
            super(null,threadName);
            setDaemon(true);
        }

        private final ProducerConsumerQueue<Boolean> q = new ProducerConsumerQueue<Boolean>();
        public void notifyTransferHandlerAsync() throws InterruptedException
        {
            q.put(Boolean.TRUE);
        }

        @Override
        public void run()
        {
            try
            {
                while (!isClosing())
                {
                    if (q.get() == null)
                        break;

                    notifyTransferHandlerSync();
                }
            }
            catch (InterruptedException e)
            {
            }
            finally
            {
                setClosed();
            }
        }
    }


}
