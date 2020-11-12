package q.rest.subscriber.dao;

import javax.ejb.Stateless;
import javax.persistence.*;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Stateless
public class DAO {

    @PersistenceContext(unitName = "QSubscriberPU")
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public <T> T findJPQLParams(Class<T> klass, String jpql, Object ... vals){
        try {
            Query q = em.createQuery(jpql);
            setVarargs(q, vals);
            return (T) q.getSingleResult();
        }catch(NoResultException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getJPQLParams(Class<T> klass, String jpql, Object ... values){
        Query q = em.createQuery(jpql);
        setVarargs(q, values);
        return (List<T>) q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getJPQLParamsMax(Class<T> klass, String jpql, int max, Object ... values){
        Query q = em.createQuery(jpql).setMaxResults(max);
        setVarargs(q, values);
        return (List<T>) q.getResultList();
    }



    @SuppressWarnings("unchecked")
    public <T> T findJPQLParamsFirst(Class<T> klass, String jpql, Object ... values){
        Query q = em.createQuery(jpql).setMaxResults(1);
        setVarargs(q, values);
        return (T) q.getSingleResult();
    }


    private void setParameter(Query q, String name, Object val) {
        if (val instanceof Date) {
            Date d = (Date) val;
            q.setParameter(name, d, TemporalType.TIMESTAMP);
        } else if (val instanceof Calendar) {
            Calendar c = (Calendar) val;
            q.setParameter(name, c.getTime(), TemporalType.TIMESTAMP);
        } else {
            q.setParameter(name, val);
        }
    }


    private void setVarargs(Query q, Object ... values){
        int i = 0;
        for(Object o : values){
            this.setParameter(q, "value"+i, o);
            i++;
        }
    }


    @SuppressWarnings("unchecked")
    public <T> List<T> get(Class<T> klass) {
        return (List<T>) em.createQuery("SELECT b FROM " + klass.getSimpleName() + " b").getResultList();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getOrderBy(Class<T> klass, String orderColumn) {
        return (List<T>) em.createQuery("SELECT b FROM " + klass.getSimpleName() + " b order by " + orderColumn).getResultList();
    }


    @SuppressWarnings("unchecked")
    public <T> List<T> getOrderByMax(Class<T> klass, String orderColumn, int max) {
        return (List<T>) em.createQuery("SELECT b FROM " + klass.getSimpleName() + " b order by " + orderColumn).setMaxResults(50).getResultList();
    }



    @SuppressWarnings("unchecked")
    public <T> List<T> getOrderByOriented(Class<T> klass, String orderColumn, String orientation) {
        return (List<T>) em.createQuery("SELECT b FROM " + klass.getSimpleName() + " b order by " + orderColumn + " " + orientation).getResultList();
    }

    public <T> List<T> getOrderByOriented(Class<T> klass, String orderColumn, String orientation, int max) {
        return (List<T>) em.createQuery("SELECT b FROM " + klass.getSimpleName() + " b order by " + orderColumn + " " + orientation).setMaxResults(max).getResultList();
    }





    public void refreshCache() {
        em.clear();
        em.getEntityManagerFactory().getCache().evictAll();
    }

    public <T> T persistAndReturn(T t) {
        em.persist(t);
        em.flush();
        return t;
    }

    public <T> void persist(T t) {
        em.persist(t);
    }

    public <T> void save(T t) {
        em.persist(t);
    }

    public void delete(Object object) {
        object = em.merge(object);
        em.remove(object);
    }

    public void insertNative(String sql) {
        Query q = em.createNativeQuery(sql);
        q.executeUpdate();
    }

    public void update(Object obj) {
        em.merge(obj);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getNative(Class<T> klass, String sql) {
        return (List<T>) em.createNativeQuery(sql, klass).getResultList();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getNativeMax(Class<T> klass, String sql, int max) {
        return (List<T>) em.createNativeQuery(sql, klass).setMaxResults(max).getResultList();
    }

    public List getNative(String sql) {
        return em.createNativeQuery(sql).getResultList();
    }

    public List getNativeMax(String sql, int max) {
        return em.createNativeQuery(sql).setMaxResults(max).getResultList();
    }

    public void updateNative(String sql) {
        em.createNativeQuery(sql).executeUpdate();
    }

    public void createNative(String sql) {
        em.createNativeQuery(sql);
    }

    public Object getNativeSingle(String sql) {
        Object o;
        try {
            o = em.createNativeQuery(sql).getSingleResult();
        } catch (NoResultException nre) {
            o = null;
        }
        return o;
    }

    @SuppressWarnings("unchecked")
    public <T> T findNative(Class<T> klass, String sql) {
        try {
            return (T) em.createNativeQuery(sql).getSingleResult();
        } catch (Exception e) {
            return null;
        }

    }

    public <T> T find(Class<T> klass, Object obj) {
        try{
            return em.find(klass, obj);
        }catch (Exception ex){
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T findCondition(Class<T> klass, String columnName, Object val) {
        Query q = em.createQuery("SELECT b FROM " + klass.getSimpleName() + " b WHERE b." + columnName + " = :value");
        this.setParameter(q, "value", val);
        List<T> list = q.getResultList();
        if (!list.isEmpty())
            return (T) list.get(0);
        else
            return null;
    }


    @SuppressWarnings("unchecked")
    public <T> List<T> getCondition(Class<T> klass, String columnName, Object val) {
        Query q = em.createQuery("SELECT b FROM " + klass.getSimpleName() + " b WHERE b." + columnName + " = :value");
        this.setParameter(q, "value", val);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getTwoConditions(Class<T> klass, String columnName, String columnName2, Object val,
                                        Object val2) {
        Query q = em.createQuery("SELECT b FROM " + klass.getSimpleName() + " b WHERE b." + columnName + " = :value"
                + " AND b." + columnName2 + "= :value2");
        this.setParameter(q, "value", val);
        this.setParameter(q, "value2", val2);
        return q.getResultList();
    }


    @SuppressWarnings("unchecked")
    public <T> T findTwoConditions(Class<T> klass, String columnName, String columnName2, Object val, Object val2) {
        try {
            Query q = em.createQuery("SELECT b FROM " + klass.getSimpleName() + " b WHERE b." + columnName + " = :value"
                    + " AND b." + columnName2 + "= :value2");
            this.setParameter(q, "value", val);
            this.setParameter(q, "value2", val2);
            return (T) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

}
