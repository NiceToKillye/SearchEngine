package loader;

import loader.scanner.Scanner;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class Loader {
    public static void main(String[] args) {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure("hibernateCfg.xml").build();
        Metadata metadata = new MetadataSources(registry)
                .getMetadataBuilder().build();
        SessionFactory sessionFactory = metadata
                .getSessionFactoryBuilder().build();
        Session session = sessionFactory.openSession();

        Scanner scanner = new Scanner(session);
        scanner.scan("http://www.playback.ru/");
    }
}
