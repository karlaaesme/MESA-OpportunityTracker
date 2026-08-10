//From class
public class Queue<T> {
    static class Node<T>{
        T data;
        Node<T> next;

        Node(T data){
            this.data = data;
            this.next = null;
        }
    }
    Node<T> front;
    Node<T> rear;
    int length;

    public Queue(){
        front = null;
        rear = null;
        length = 0;
    }

    public void enqueue(T data){
        Node<T> newNode = new Node<>(data);
        if(rear == null){
            front = newNode;
        } else {
            rear.next = newNode;
        }
        rear = newNode;
        length++;
    }
    public T dequeue(){
        if(front == null){
            System.out.println("No opp on queue");
            return null;
        }
        T value = front.data;
        front = front.next;

        if(front == null){
            rear = null;
        }
        length--;
        return value;
    }
    public T peek(){
        if(front == null)
            return null;
        return front.data;
    }
    public boolean isEmpty(){
        return front == null;
    }
    public int size(){
        return length;
    }
}