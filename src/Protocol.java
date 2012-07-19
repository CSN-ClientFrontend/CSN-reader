
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

    static public class Request {
        enum TypeOfRequest {
            RequestData, RequestSerials
        }

        TypeOfRequest type;
    }

}
