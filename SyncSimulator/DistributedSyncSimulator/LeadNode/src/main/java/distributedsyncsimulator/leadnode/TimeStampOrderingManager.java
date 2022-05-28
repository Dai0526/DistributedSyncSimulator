package distributedsyncsimulator.leadnode;

import java.util.*;

import distributedsyncsimulator.utilities.*;
import distributedsyncsimulator.shared.*;
import static distributedsyncsimulator.utilities.Constants.*;


public class TimeStampOrderingManager extends SyncManagerBase {
    public TimeStampOrderingManager(){
        super();
    }

    @Override
    public ArrayList<String> releaseLocks(MyTransaction trans){
        MyLog.instance().log("Start Release Lock for Transaction " + trans.m_id + NEWLINE);

        
        return new ArrayList<String>();
    }

    @Override
    public boolean acquireLocks(MyAction act){
        MyLog.instance().log("Start Acqure Lock for act = " + act + NEWLINE);

        boolean stats = false;
        String target = act.m_target;

        MyLock lk = act.getLock();
        if(checkLock(lk)){
            MyLock prev = getLock(act.m_tanscationId, target);
            if(prev == null){
                MyLog.instance().log("\tNo prev lock, set new lock" + NEWLINE);
                setLock(lk);
            }else{
                MyLog.instance().log("\tfind prev lock, updated Lock" + NEWLINE);
                prev.updateLock(lk.m_type);  
            }
            stats = true;

        }else{
            // add new act to queues
            MyLog.instance().log("\tPut lock to waiting queue" + NEWLINE);
            if(!m_acts.containsKey(target)){
                m_acts.put(target, new ArrayList<MyAction>());
            }
            m_acts.get(target).add(act);
            stats = false;
        }
        
        return stats;
    }


     /*
        A transaction 
            try to READ X:
               If WT(X) > TS(T) then ROLLBACK
                Else READ and update RT(X) to larger of TS(T) or RT(X) 
            try to WRITE X:
                If RT(X) > TS(T) then ROLLBACK
                Else if WT(X) > TS(T) ignore write & continue (Thomas Write Rule)
                Otherwise, WRITE and update WT(X) =TS(T)             
    */
    private boolean checkLock(MyLock lk){
        MyLog.instance().log("Start Compare Lock " + lk + NEWLINE);

        String target = lk.m_target;
        ArrayList<MyLock> locks = m_locks.get(target);

        if(locks == null){
            MyLog.instance().log("\tIt's a new Lock, Can be acquired" + NEWLINE);
            return true;
        }

        switch(lk.m_type){
            case READ:
                for(MyLock curr : locks){
                    if(curr.m_tansId.compareTo(lk.m_tansId) != 0 && curr.m_type.compareTo(MyLock.LockType.READ) > 0){
                        MyLog.instance().log("\tCannot acquire, different transaction try to lock the same item with Lock Level higher than READ (READ, WRITE Conflict)." + NEWLINE);
                        return false;
                    }
                }
                break;
            case WRITE:
                for(MyLock curr : locks){
                    if(curr.m_tansId.compareTo(lk.m_tansId) != 0){
                        MyLog.instance().log("\tCannot acquire, cannot have more than 1 transasction write to one item" + NEWLINE);
                        return false;
                    }
                }
                break;
            default:
                return false;
        }

        MyLog.instance().log("\tLock is acquirable" + NEWLINE);
        return true;
    }
}