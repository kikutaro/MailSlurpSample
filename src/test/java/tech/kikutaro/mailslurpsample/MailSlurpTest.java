package tech.kikutaro.mailslurpsample;

import tech.kikutaro.mailslurpsample.model.GetResponse;
import tech.kikutaro.mailslurpsample.model.PostResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.IOException;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import static org.hamcrest.CoreMatchers.is;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * MailSlurpを利用したメールのEnd-To-Endテスト
 * 
 * @author kikuta
 */
public class MailSlurpTest {
    
    private final static String MAILSLURP_API_KEY = "";
    private final static String SENDGRID_API_KEY = "";

    @Before
    public void setUp() {
        Unirest.setObjectMapper(new ObjectMapper() {
            private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper
                    = new com.fasterxml.jackson.databind.ObjectMapper();

            @Override
            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return jacksonObjectMapper.readValue(value, valueType);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String writeValue(Object value) {
                try {
                    return jacksonObjectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Test
    public void MailEndToEndTest() throws UnirestException, MessagingException, InterruptedException {
        //MailSlurpにinboxを作成
        HttpResponse<PostResponse> postRet = Unirest.post(" https://api.mailslurp.com/inboxes")
                .queryString("apiKey", MAILSLURP_API_KEY)
                .asObject(PostResponse.class);
        
        //inboxの宛先
        String testTo = postRet.getBody().getPayload().getAddress();
        
        //JavaMailからSendGridのSMTPを利用してメール送信
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.sendgrid.net");
        props.put("mail.smtp.port", 587);
        props.put("mail.smtp.auth", true);

        Session session = Session.getInstance(props, new Authenticator(){
            @Override
            protected PasswordAuthentication getPasswordAuthentication(){
                return new PasswordAuthentication("apikey", SENDGRID_API_KEY);
            }
        });
        session.setDebug(true);
        
        Message msg = new MimeMessage(session);
        msg.setSubject("subject");
        msg.setContent("hello", "text/plain");
        Address from = new InternetAddress("from@kikutaro.xyz");
        msg.setFrom(from);
        Address to = new InternetAddress(testTo);
        msg.setRecipient(RecipientType.TO, to);
        msg.setHeader("X-Mailer", "JavaMail");

        //メール送信
        Transport.send(msg);
        
        //MailSlurpのinbox受信待ちのためにウェイト
        Thread.sleep(20000);
        
        //inboxで受信確認
        HttpResponse<GetResponse> getRes = Unirest.get("https://api.mailslurp.com/inboxes/{id}")
                .routeParam("id", postRet.getBody().getPayload().getId())
                .queryString("apiKey", MAILSLURP_API_KEY)
                .asObject(GetResponse.class);
        
        getRes.getBody().getPayload().stream().forEach(p -> {
            assertThat(p.getSubject(), is("subject"));
            System.out.println(p.getBody());
        });
    }
}
