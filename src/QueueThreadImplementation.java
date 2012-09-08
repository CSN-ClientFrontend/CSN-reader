import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


class QueueObject
{
    long timeBetween;
    long lastTime;
    String url;
    int port;
    long id;
}


public class QueueThreadImplementation implements Runnable {

    
    
    long lastId = 0;
    
    List<QueueObject> requestQueue;
    
    final Lock requestQueueLock = new ReentrantLock();
    
    final Condition waitingForNewRequests = requestQueueLock.newCondition();
    
    StorageDatabase data = new StorageDatabase();
    
    long getShortestWaitTime()
    {
        long shortestTime = Long.MAX_VALUE;
        for (QueueObject obj: requestQueue)
        {
            long timeTill =  (obj.timeBetween + obj.lastTime) - System.currentTimeMillis();
            shortestTime = Math.min(timeTill, shortestTime);
        }
        
        return shortestTime;
        
    }
    
    @Override
    public void run() {
        
        requestQueue = data.getQueueListItems();
        
        
        while (true)
        {
            requestQueueLock.lock();
            
            
            
            try {
                waitingForNewRequests.await(getShortestWaitTime(), TimeUnit.MILLISECONDS);
                
                processAllOutstandingRequests();
                
                
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            finally
            {
                requestQueueLock.unlock();
            }
        }

    }
    
    long addQueueObject(QueueObject objectToAdd)
    {
        requestQueueLock.lock();
        
        long idToUse = data.addQueueListItem(objectToAdd);
        objectToAdd.id = idToUse;
        
        requestQueue.add(objectToAdd);
        
        waitingForNewRequests.signal();
        
        requestQueueLock.unlock();
        
        return idToUse;
        
    }
    
    QueueObject[] getAllQueueObjects()
    {
       requestQueueLock.lock();
       
       QueueObject[] results =  requestQueue.toArray(new QueueObject[0]);
       
       requestQueueLock.unlock();
       
       return results;
    }
    
    boolean removeQueueObject(long id)
    {
        requestQueueLock.lock();
        
        data.removeQueueListItem(id);
        
        for (Iterator<QueueObject> it = requestQueue.iterator(); it.hasNext();)
        {
            QueueObject next = it.next();
            
            if (next.id == id)
            {
                it.remove();
                requestQueueLock.unlock();
                return true;
            }
        }
        
        requestQueueLock.unlock();
        
        return false;
    }

    private void processAllOutstandingRequests() {
        for (QueueObject obj: requestQueue)
        {
            long timeTill =  (obj.timeBetween + obj.lastTime) - System.currentTimeMillis();
           
            if (timeTill <= 0)
            {
                System.out.println("A request has been completed " +obj.url + ":"+ obj.port);
                obj.lastTime = System.currentTimeMillis();
                data.updateQueueListLastTime(obj.lastTime, obj.id);
            }
            
            
        }
        
    }

}
