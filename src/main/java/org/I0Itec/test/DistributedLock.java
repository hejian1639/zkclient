package org.I0Itec.test;  

import java.util.concurrent.TimeUnit;  

public interface DistributedLock {  
      
    /* 
     * ��ȡ�������û�еõ��͵ȴ� 
     */  
    public void acquire() throws Exception;  

    /* 
     * ��ȡ����ֱ����ʱ 
     */  
    public boolean acquire(long time, TimeUnit unit) throws Exception;  

    /* 
     * �ͷ��� 
     */  
    public void release() throws Exception;  


}  