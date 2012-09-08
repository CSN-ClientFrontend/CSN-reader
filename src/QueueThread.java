
public class QueueThread {
    
    
    
    QueueThreadImplementation queueThreadImplementation = new QueueThreadImplementation();
    Thread innerThread= new Thread(queueThreadImplementation);
    public void start()
    {
        
        innerThread.start();
    }
    
    
    long addQueueObject(QueueObject objectToAdd) {
        return queueThreadImplementation.addQueueObject(objectToAdd);
    }
    
    QueueObject[] getAllQueueObjects(){
        return queueThreadImplementation.getAllQueueObjects();
    }
    
    boolean removeQueueObject(long id){
        return queueThreadImplementation.removeQueueObject(id);
    }

}
