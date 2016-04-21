package rmi;
import java.lang.reflect.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.*;
import java.util.*;

public class RMIInvocationHandler implements InvocationHandler {
    InetSocketAddress skeletonAddress;
    Class<?> intf;

    public RMIInvocationHandler(Class<?> c, InetSocketAddress address) {
        this.intf = c;
        this.skeletonAddress = address;
    }

    public InetSocketAddress getInetSocketAddress() {
        return this.skeletonAddress;
    }

    public Class<?> getIntf() {
        return this.intf;
    }

    public boolean equalsStub(Object stub) {
        if(stub == null) {
            return false;
        }

        InvocationHandler stubhandler = Proxy.getInvocationHandler(stub);


        try {
            // Invoke getInetSocketAddress method from stub
            Method inetMethod = RMIInvocationHandler.class.getMethod("getInetSocketAddress");
            @SuppressWarnings("unchecked")
            InetSocketAddress address = (InetSocketAddress) stubhandler.invoke(stub, inetMethod, new Object[]{});

            // Invoke getIntf method from stub
            Method intfMethod = RMIInvocationHandler.class.getMethod("getIntf");
            @SuppressWarnings("unchecked")
            Class<?> intf = (Class<?>) stubhandler.invoke(stub, intfMethod, new Object[]{});
            if(this.skeletonAddress.toString().equals(address.toString())
                    && this.intf.toString().equals(intf.toString())) {
                return true;
            }
            else {
                return false;
            }
        }
        catch(NoSuchMethodException e) {
            return false;
        }
        catch(Throwable e) {
            return false;
        }
    }

    public int hashCodeStub() {
        //System.out.println("-----------------------" + this.intf.toString());
        return (this.intf.toString() + this.skeletonAddress.toString()).hashCode();
    }

    public String toStringStub() {
        return this.intf.toString();
    }

    // TODO: there should be more constructors

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
        Socket socket = null;
        ObjectOutputStream objOutput = null;
        ObjectInputStream objInput = null;

        System.out.println(method.getName());

        switch(method.getName()) {
            case "equals":                  return this.equalsStub(args[0]);
            case "hashCode":                return this.hashCodeStub();
            case "toString":                return this.toStringStub();
            case "getInetSocketAddress":    return this.getInetSocketAddress();
            case "getIntf":                 return this.getIntf();
            default:                        break;
        }

        try {
            System.out.println("---------------" + this.skeletonAddress.toString());
            socket = new Socket(this.skeletonAddress.getAddress(),
                                       this.skeletonAddress.getPort());
            objOutput = new ObjectOutputStream(socket.getOutputStream());
            objOutput.flush();
            objInput = new ObjectInputStream(socket.getInputStream());

            System.out.println("--------------- Connected!");

            System.out.println("\n\n---Writing " + method.getName());
            objOutput.writeObject(method.getName());
            System.out.println("\n\n---Writing " + method.getParameterTypes());
            objOutput.writeObject(method.getParameterTypes());
            System.out.println("\n\n---Writing " + Arrays.toString(args));
            objOutput.writeObject(args);
            //objOutput.flush();

            result = objInput.readObject();
        }
        catch(UnknownHostException e) {
            //System.out.println("------------ RMIInvocation");
            throw new RMIException(e);
        }
        catch(IOException e) {
            System.out.println("------------ RMIInvocation");
            throw new RMIException(e);
        }
        finally {
            if(objInput != null) objInput.close();
            if(objOutput != null) objOutput.close();
            if(socket != null) socket.close();
        }
        if(result instanceof InvocationTargetException) {
            throw ((InvocationTargetException) result).getTargetException();
        }
        return result;
    }
}
