package io.quarkus.qe.hibernate.search;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * Don't use this ID generator in prod. Was developed just for testing proposes
 */
public class MyCustomIdGenerator implements IdentifierGenerator {

    private Random rand = new Random();
    private Set<Integer> ids = new HashSet<>();

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object obj) throws HibernateException {
        return unique(rand.nextInt(Integer.MAX_VALUE));
    }

    private int unique(int id) {
        while (ids.contains(id)) {
            id = rand.nextInt(Integer.MAX_VALUE);
        }
        ids.add(id);
        return id;
    }
}
