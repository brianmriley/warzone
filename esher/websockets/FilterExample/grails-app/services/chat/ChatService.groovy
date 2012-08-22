package chat

import org.atmosphere.cpr.AtmosphereRequest
import org.atmosphere.cpr.AtmosphereResource
import org.atmosphere.cpr.AtmosphereResource.TRANSPORT
import org.atmosphere.cpr.AtmosphereResponse
import grails.converters.JSON
import org.atmosphere.cpr.MetaBroadcaster
import java.util.concurrent.TimeUnit
import java.util.concurrent.Callable

class ChatService {

    static transactional = false
    static atmosphere = [mapping: '/atmosphere/chat']

    ChatService(){
        MetaBroadcaster.default.scheduleTo("/chat/pie",
           new Callable(){
               Object call() {
                   println "Sending scheduled message to 'pie' destination"
                   return "I Love Pie! It is now ${new Date()}"
               }
           },
            10, TimeUnit.SECONDS)
        println "Pie broadcast scheduled"
    }

    def onRequest = { event ->
        println "Inside onRequest!"
        try {
            AtmosphereRequest req = event.request
            if (req.method.equalsIgnoreCase("GET")) {
                event.suspend()
            } else if (req.method.equalsIgnoreCase("POST")) {
                event.broadcaster.broadcast(req.reader.readLine().trim())
            }
        } catch (Exception e) {
            println "ERROR!!!!!"
        }

    }

    def onStateChange = { event ->
        println "Inside onStageChange"
        AtmosphereResource r = event.resource
        AtmosphereResponse res = r.response

        try {
            if (event.isSuspended()) {
                def msg = JSON.parse(event.message)
                res.writer.write( createMessage(msg.author, msg.message) )

                switch (r.transport()) {
                    case TRANSPORT.JSONP:
                    case TRANSPORT.LONG_POLLING:
                        event.resource.resume()
                        break
                    default:
                        res.writer.flush()
                }
            } else if (!event.isResuming()) {
                event.broadcaster().broadcast( createMessage('someone', 'buh bye') )
            }
        } catch (Exception e) {
            println "ERROR in onStateChange: $e"
        }
    }

    private String createMessage(String author, String text) {
        return new JSON( [text : text, author : author, time : new Date().time] )
    }
}
