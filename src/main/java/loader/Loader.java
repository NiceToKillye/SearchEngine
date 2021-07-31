package loader;

import loader.entity.Field;
import loader.scanner.Scanner;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.io.IOException;

public class Loader {
    public static void main(String[] args) throws IOException {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure("hibernateCfg.xml").build();
        Metadata metadata = new MetadataSources(registry)
                .getMetadataBuilder().build();
        SessionFactory sessionFactory = metadata
                .getSessionFactoryBuilder().build();
        Session session = sessionFactory.openSession();

        createField(session);

        Scanner scanner = new Scanner(session);
        scanner.scan("http://www.playback.ru/");
    }

    private static void createField(Session session){
        Field firstField = new Field("title", "title", 1.0f);
        Field secondField = new Field("body", "body", 0.8f);

        try {
            session.beginTransaction();
            session.saveOrUpdate(firstField);
            session.saveOrUpdate(secondField);
            session.getTransaction().commit();
        }
        catch (Exception exception){
            session.getTransaction().rollback();
            System.out.println("WARNING: The transaction was not completed!");
        }
    }
}
