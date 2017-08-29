package org.I0Itec.test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.I0Itec.zkclient.ZkClient;

public class SimpleDistributedLockMutex extends BaseDistributedLock implements DistributedLock{

    private static final String LOCK_NAME = "lock-";
    
    private final String basePath; 
    
    private String ourLockPath;
    
    private boolean internalLock(long time, TimeUnit unit) throws Exception{
        ourLockPath = attemptLock(time, unit);
        return ourLockPath != null;
    }
    
    public SimpleDistributedLockMutex(ZkClient client, String path) {
        super(client, path, LOCK_NAME);
        basePath = path;
    }

    @Override
    public void acquire() throws Exception {
        if(!internalLock(-1, null)){
            throw new IOException("connection lost! path: "+basePath+" can't get lock");
        }
    }

    @Override
    public boolean acquire(long time, TimeUnit unit) throws Exception {
        return internalLock(time, unit);
    }

    @Override
    public void release() throws Exception {
        releaseLock(ourLockPath);        
    }

}

