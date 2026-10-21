package com.example.moneylens;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

import static org.junit.Assert.*;

public class CustomerSerializationTest {

    @Test
    public void testCustomerSerializationWithTimestamp() throws Exception {
        Customer original = new Customer("c1", "Alice", "9876543210", 150.0, Timestamp.now());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(original);
        oos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Customer deserialized = (Customer) ois.readObject();
        ois.close();

        assertNotNull(deserialized);
        assertEquals(original.getId(), deserialized.getId());
        assertEquals(original.getName(), deserialized.getName());
        assertEquals(original.getPhone(), deserialized.getPhone());
        assertEquals(original.getBalance(), deserialized.getBalance(), 0.001);
        assertNotNull(deserialized.getCreatedAt());
        assertEquals(original.getCreatedAt().getSeconds(), deserialized.getCreatedAt().getSeconds());
    }

    @Test
    public void testCustomerTransactionSerialization() throws Exception {
        CustomerTransaction original = new CustomerTransaction(
                "tx1", "c1", 500.0, "Payment", Timestamp.now(), "gave", "link1");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(original);
        oos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        CustomerTransaction deserialized = (CustomerTransaction) ois.readObject();
        ois.close();

        assertNotNull(deserialized);
        assertEquals(original.getId(), deserialized.getId());
        assertEquals(original.getCustomerId(), deserialized.getCustomerId());
        assertEquals(original.getAmount(), deserialized.getAmount(), 0.001);
        assertNotNull(deserialized.getDate());
        assertEquals(original.getDate().getSeconds(), deserialized.getDate().getSeconds());
    }

    @Test
    public void testTransactionSerialization() throws Exception {
        Transaction original = new Transaction(
                "t1", 250.0, "expense", "Food", "Dinner", new Date(),
                "UPI", "INR", "₹", "manual", null, Timestamp.now());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(original);
        oos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Transaction deserialized = (Transaction) ois.readObject();
        ois.close();

        assertNotNull(deserialized);
        assertEquals(original.getId(), deserialized.getId());
        assertEquals(original.getAmount(), deserialized.getAmount(), 0.001);
        assertNotNull(deserialized.getCreatedAt());
        assertEquals(original.getCreatedAt().getSeconds(), deserialized.getCreatedAt().getSeconds());
    }

    @Test
    public void testSavingsGoalSerialization() throws Exception {
        SavingsGoal original = new SavingsGoal(
                "sg1", "Car", "🚗", 100000.0, 20000.0, "INR",
                Timestamp.now(), Timestamp.now());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(original);
        oos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        SavingsGoal deserialized = (SavingsGoal) ois.readObject();
        ois.close();

        assertNotNull(deserialized);
        assertEquals(original.getId(), deserialized.getId());
        assertNotNull(deserialized.getDeadline());
        assertNotNull(deserialized.getCreatedAt());
        assertEquals(original.getDeadline().getSeconds(), deserialized.getDeadline().getSeconds());
        assertEquals(original.getCreatedAt().getSeconds(), deserialized.getCreatedAt().getSeconds());
    }
}
