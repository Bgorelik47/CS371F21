package framework;
import utils.BoundedBuffer;
import java.util.LinkedList;
import java.util.ArrayList;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
public class MyMapReduce extends MapReduce {
	//TODO: your code here. Define all attributes
	//What is in a running instance of MapReduce?
	private PartitionTable pTable;
	private ConcurrentKVStore kvStore;
	private MapperReducerClientAPI client;
	private int numMappers;
	private int numReducers;
	private Lock lock = new ReentrantLock(); //currently very coarse granularity
	private boolean allMappersDone = false;
	private int numMappersDone = 0;
	private Thread[] mappers;
	private Thread[] reducers;

	public static class KVPair {
		Object key;
		Object value;
		public KVPair(Object key, Object value) {
			this.key = key;
			this.value = value;
		}
	}
	public static class EOS {
		int key;
		public EOS(int pNum) {
			key = pNum;
		}
	}

	public void MREmit(Object key, Object value)  //KV from File-->PartitionTable
	{
		//TODO: your code here. Delete UnsupportedOperationException after your implementation is done.
		int pNum;
		if(key instanceof EOS) {
			pNum = ((EOS) key).key;
			key = null;
		} else {
			pNum = (int)client.Partitioner(key, numMappers);
		}
		try {
			pTable.partitions[pNum].deposit(key, value);



		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		/*From assignment doc:
		The MREmit() function is thus another key part of your class; it needs to take key/value
		pairs from the many different mappers and store them in a way that later reducers can
		access them, given constraints described below. Designing and implementing this data
		structure is thus a central challenge of the project: we will use  bounded buffer we
		learned in class to transfer data between mappers and reducers.
		 */
		//throw new UnsupportedOperationException();
	}
	private LinkedList intermediateReduce(PartitionTable.Partition partition) {
		//LOGGER.log(Level.INFO, Thread.currentThread().getName() + " start intermediateReduce");
		LinkedList<Object> uniqueKeys = new LinkedList();

		KVPair kv;
		int i = 0;
		try {
			kv = (KVPair)partition.fetch();
			while (kv.key != null) {

				if(i % 500 == 0 && i >= 500) {
					/*
					LOGGER.log(Level.INFO,
							Thread.currentThread().getName() + " move " + kv.key + " to kvStore");

					 */
				}
				i++;

				kvStore.put(kv.key, kv.value);
				if (!uniqueKeys.contains(kv.key)) {
					uniqueKeys.add(kv.key);
				}
				kv = (KVPair)partition.fetch();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return uniqueKeys;

	}

	public Object MRGetNext(Object key, int partition_number) {
		//TODO: your code here. Delete UnsupportedOperationException after your implementation is done.
		/*From assignment doc:
		After the mappers are finished, your class should have stored the key/value pairs in such a
		way that the Reduce() function can be called.Reduce() is invoked once per key and is passed
		the key along with a function that enables iteration over all of the values that produced
		that same key. To iterate, the code just calls MRGetNext() repeatedly until a NULL value is
		returned;  MRGetNext() returns a value object passed in by the MREmit() function above, or
		NULL when the key's values have been processed.
		 */
		//LOGGER.log(Level.INFO, "now reducing " + key + " " + partition_number);
		LinkedList<Object> values = new LinkedList<Object>();
		Object v = kvStore.remove(key);
		if(v == null) {
			return null;
		}
		else {
			return v;
		}
		/*
		while(v != null) {
			values.add(v);
			v = kvStore.remove(key);
		}

		return values;
				 */
		//throw new UnsupportedOperationException();
	}
	@Override
	protected void MRRunHelper(String inputFileName,
							   MapperReducerClientAPI mapperReducerObj,
							   int num_mappers,
							   int num_reducers)
	{
		//TODO: your code here. Delete UnsupportedOperationException after your implementation is done.

		//our control flow?
		//instantiates stuff
		pTable = new PartitionTable(num_mappers);
		kvStore = new ConcurrentKVStore();
		client = mapperReducerObj;
		//client.Map(inputFileName);
		numMappers = num_mappers;
		numReducers = num_reducers;
		// creates and calls threads
		//steps:
		//create i mapper threads which do function mapperReducerObj.Map(pw[0...i])
		//after a mapper thread calls Map(pw[i]); call MREmit(null, null);
		//create i reducer threads which do internal function reduce(pt[0...i]) that moves
		// KVs from pt[i]-->ConcurrentStore then calls user-defined Reduce(key, i) for each
		// key in pt[i]
		mappers = new Thread[numMappers];
		reducers = new Thread[numReducers];
		String splitFile;
		for(int i = 0; i < numMappers; i++) {
			splitFile = String.format(inputFileName + ".%02d", i);
			mappers[i] = new Thread(new Mapper(splitFile, pTable, client));
			mappers[i].setName("Mapper-" + i);
			//mappers[i].setPriority(10);
			mappers[i].start();
		}
		for(int i = 0; i < numReducers; i++) {
			reducers[i] = new Thread(new Reducer(pTable.partitions[i], i, client));
			reducers[i].setName("Reducer-" + i);
			//reducers[i].setPriority(5);
			reducers[i].start();
		}



		for(int i = 0; i < numMappers; i++) {
			try {
				mappers[i].join();
			} catch(Exception e) {
				LOGGER.log(Level.INFO, "line 136");
			}

		}
		for(int i = 0; i < numReducers; i++) {
			try {
				reducers[i].join();
			} catch(Exception e) {
				LOGGER.log(Level.INFO, "line 136");
			}

		}



		/*
			Map(file)-->{while (token=file.nextLine)!=null->MREmit(token, "1")}

			Reduce(key, pNum)-->count=0-->{while MRGetNext(key, pNum) != null)
			-->count++-->stats[pNum]++}-->MRPostProcess(key, count, pNum)
		 */
		//throw new UnsupportedOperationException();
	}


	private class Mapper extends Thread  {
		PartitionTable pTable;
		String inputSource;
		MapperReducerClientAPI client;

		Mapper(String inputSource, PartitionTable pTable, MapperReducerClientAPI client) {
			this.pTable = pTable;
			this.inputSource = inputSource;
			this.client = client;
			/**/
			//run();
		}

		@Override
		public void run() {
			//create i mapper threads which do function mapperReducerObj.Map(pw[0...i])
			LOGGER.log(Level.INFO, Thread.currentThread().getName() + " started");
			client.Map(inputSource);
			lock.lock();
			numMappersDone++;
			try {
				if (numMappersDone == numMappers) {
					allMappersDone = true;
					for (int i = 0; i < pTable.size(); i++) {
						EOS eos = new EOS(i);
						MREmit(eos, null);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				lock.unlock();
			}
			LOGGER.log(Level.INFO, Thread.currentThread().getName() + " complete");
		}
	}
	private class Reducer implements Runnable {
		private PartitionTable.Partition partition;
		int pNum;
		MapperReducerClientAPI client;

		Reducer(PartitionTable.Partition p, int i, MapperReducerClientAPI client) {
			this.partition = p;
			this.pNum = i;
			this.client = client;
			//run();
		}
		@Override
		public void run() {
			LOGGER.log(Level.INFO, Thread.currentThread().getName() + " started");
			//do intermediate reduce:
			LinkedList<Object> uniqueKeys = intermediateReduce(partition);
			Object key;
			//intermediateReduce will not return until all mappers are done
			//do user reduce for each unique key in pTable

			LOGGER.log(Level.INFO, Thread.currentThread().getName() + " intermediateReduce " +
					"complete");

			while(((key = uniqueKeys.pollFirst()) != null)) {
				//LOGGER.log(Level.INFO, "" + Thread.currentThread().getName() + "started key " +
				// key);
				client.Reduce(key, pNum);
			}
			LOGGER.log(Level.INFO, Thread.currentThread().getName() + " complete");




			//create i reducer threads which do internal function reduce(pt[0...i]) that moves
			// KVs from pt[i]-->ConcurrentStore then calls user-defined Reduce(key, i) for each
			// key in pt[i]


		}
	}
}
