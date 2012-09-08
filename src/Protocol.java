
public abstract class Protocol {

    public static class RequestData {
        static public class RequestMessageParameters {
            long startTime;
            long endTime;
            int resolution;
            int serialNumber;
        }

        static public class ResponseMetadata {
            SectionMetada[] sections;
        }

        static public class SectionMetada {
            long length;
            long startTime;
            long endTime;
        }
    }

    static public class RequestSerials {
        static public class SerialsResponse {
            int[] serialNumbers;
        }
    }
    
    
    static public class PushQueue {
        static public class PushQueueAddRequest
        {
            long timeBetween;
            String url;
            int port;
            
        }
        
        static public class PushQueueAddResponse
        {
            long id;
        }
        
        static public class PushQueueRemoveRequest
        {
            long id;
        }
        
        static public class PushQueueRemoveResponse
        {
            boolean success;
        }
        
        static public class PushQueueItem
        {
            long timeBetween;
            long lastTime;
            String url;
            int port;
            long id;
        }
        
        static public class PushQueueDisplayResponse
        {
            PushQueueItem[] items;
        }
    }

    static public class Request {
        enum TypeOfRequest {
            RequestData, RequestSerials, PushQueueDisplayRequest, PushQueueAddRequest, PushQueueRemoveRequest
        }

        TypeOfRequest type;
    }
    
    

}
