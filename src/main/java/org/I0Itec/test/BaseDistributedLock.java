package org.I0Itec.test;  

import java.util.Collections;  
import java.util.Comparator;  
import java.util.List;  
import java.util.concurrent.CountDownLatch;  
import java.util.concurrent.TimeUnit;  

import org.I0Itec.zkclient.IZkDataListener;  
import org.I0Itec.zkclient.ZkClient;  
import org.I0Itec.zkclient.exception.ZkNoNodeException;  

public class BaseDistributedLock {  
      
    private final ZkClient client;  
    private final String  path;  
      
    //zookeeper��locker�ڵ��·��  
    private final String  basePath;  
    private final String  lockName;  
    private static final Integer  MAX_RETRY_COUNT = 10;  
          
    public BaseDistributedLock(ZkClient client, String path, String lockName){  

        this.client = client;  
        this.basePath = path;  
        this.path = path.concat("/").concat(lockName);        
        this.lockName = lockName;  
          
    }  
      
    private void deleteOurPath(String ourPath) throws Exception{  
        client.delete(ourPath);  
    }  
      
    private String createLockNode(ZkClient client,  String path) throws Exception{  
          
        return client.createEphemeralSequential(path, null);  
    }  
      
    private boolean waitToLock(long startMillis, Long millisToWait, String ourPath) throws Exception{  
          
        boolean  haveTheLock = false;  
        boolean  doDelete = false;  
          
        try  
        {  

            while ( !haveTheLock )  
            {  
                //��ȡlock�ڵ��µ����нڵ�  
                List<String> children = getSortedChildren();  
                String sequenceNodeName = ourPath.substring(basePath.length()+1);  

                //��ȡ��ǰ�ڵ�������нڵ��б��е�λ��  
                int  ourIndex = children.indexOf(sequenceNodeName);  
                //�ڵ�λ��С��0,˵��û���ҵ��ڵ�  
                if ( ourIndex<0 ){  
                    throw new ZkNoNodeException("�ڵ�û���ҵ�: " + sequenceNodeName);  
                }  
                  
                //�ڵ�λ�ô���0˵�����������ڵ��ڵ�ǰ�Ľڵ�ǰ�棬����Ҫ�ȴ������Ľڵ㶼�ͷ�  
                boolean isGetTheLock = ourIndex == 0;  
                String  pathToWatch = isGetTheLock ? null : children.get(ourIndex - 1);  

                if ( isGetTheLock ){  
                      
                    haveTheLock = true;  
                      
                }else{  
                    /** 
                     * ��ȡ��ǰ�ڵ�Ĵ�С�Ľڵ㣬�������ڵ�ı仯 
                     */  
                    String  previousSequencePath = basePath .concat( "/" ) .concat( pathToWatch );  
                    final CountDownLatch    latch = new CountDownLatch(1);  
                    final IZkDataListener previousListener = new IZkDataListener() {  
                          
                        public void handleDataDeleted(String dataPath) throws Exception {  
                            latch.countDown();            
                        }  
                          
                        public void handleDataChange(String dataPath, Object data) throws Exception {  
                            // ignore                                     
                        }  
                    };  

                    try   
                    {                    
                        //����ڵ㲻���ڻ�����쳣  
                        client.subscribeDataChanges(previousSequencePath, previousListener);  
                          
                        if ( millisToWait != null )  
                        {  
                            millisToWait -= (System.currentTimeMillis() - startMillis);  
                            startMillis = System.currentTimeMillis();  
                            if ( millisToWait <= 0 )  
                            {  
                                doDelete = true;    // timed out - delete our node  
                                break;  
                            }  

                            latch.await(millisToWait, TimeUnit.MICROSECONDS);  
                        }  
                        else  
                        {  
                            latch.await();  
                        }  
                    }  
                    catch ( ZkNoNodeException e )   
                    {  
                        //ignore  
                    }finally{  
                        client.unsubscribeDataChanges(previousSequencePath, previousListener);  
                    }  

                }  
            }  
        }  
        catch ( Exception e )  
        {  
            //�����쳣��Ҫɾ���ڵ�  
            doDelete = true;  
            throw e;  
        }  
        finally  
        {  
            //�����Ҫɾ���ڵ�  
            if ( doDelete )  
            {  
                deleteOurPath(ourPath);  
            }  
        }  
        return haveTheLock;  
    }  
      
    private String getLockNodeNumber(String str, String lockName)  
    {  
        int index = str.lastIndexOf(lockName);  
        if ( index >= 0 )  
        {  
            index += lockName.length();  
            return index <= str.length() ? str.substring(index) : "";  
        }  
        return str;  
    }  
      
    List<String> getSortedChildren() throws Exception  
    {  
        try{  
              
            List<String> children = client.getChildren(basePath);  
            Collections.sort  
            (  
                children,  
                new Comparator<String>()  
                {  
                    public int compare(String lhs, String rhs)  
                    {  
                        return getLockNodeNumber(lhs, lockName).compareTo(getLockNodeNumber(rhs, lockName));  
                    }  
                }  
            );  
            return children;  
              
        }catch(ZkNoNodeException e){  
              
            client.createPersistent(basePath, true);  
            return getSortedChildren();  
              
        }  
    }  
      
    protected void releaseLock(String lockPath) throws Exception{  
        deleteOurPath(lockPath);      
          
    }  
      
    /** 
     * ���Ի�ȡ�� 
     * @param time 
     * @param unit 
     * @return 
     * @throws Exception 
     */  
    protected String attemptLock(long time, TimeUnit unit) throws Exception{  
          
        final long      startMillis = System.currentTimeMillis();  
        final Long      millisToWait = (unit != null) ? unit.toMillis(time) : null;  

        String          ourPath = null;  
        boolean         hasTheLock = false;  
        boolean         isDone = false;  
        int             retryCount = 0;  
          
        //����������Ҫ����һ��  
        while ( !isDone )  
        {  
            isDone = true;  

            try  
            {  
                ourPath = createLockNode(client, path);  
                hasTheLock = waitToLock(startMillis, millisToWait, ourPath);  
            }  
            catch ( ZkNoNodeException e )  
            {  
                if ( retryCount++ < MAX_RETRY_COUNT )  
                {  
                    isDone = false;  
                }  
                else  
                {  
                    throw e;  
                }  
            }  
        }  
        if ( hasTheLock )  
        {  
            return ourPath;  
        }  

        return null;  
    }  
      
      
}  