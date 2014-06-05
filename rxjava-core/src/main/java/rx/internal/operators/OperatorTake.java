/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.operators;

import rx.Observable.Operator;
import rx.Subscriber;

/**
 * Returns an Observable that emits the first <code>num</code> items emitted by the source Observable.
 * <p>
 * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/Netflix/RxJava/images/rx-operators/take.png" />
 * <p>
 * You can choose to pay attention only to the first <code>num</code> items emitted by an Observable by using
 * the {@code take} operation. This operation returns an Observable that will invoke a subscribing Observer's
 * {@code onNext} function a maximum of {@code num} times before invoking {@code onCompleted}.
 */
public final class OperatorTake<T> implements Operator<T, T> {

    final int limit;

    public OperatorTake(int limit) {
        this.limit = limit;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        Subscriber<T> parent = new Subscriber<T>() {

            int count = 0;
            boolean completed = false;

            @Override
            public void onCompleted() {
                if (!completed) {
                    child.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!completed) {
                    child.onError(e);
                }
            }

            @Override
            public void onNext(T i) {
                if (!isUnsubscribed()) {
                    child.onNext(i);
                    if (++count >= limit) {
                        completed = true;
                        child.onCompleted();
                        unsubscribe();
                    }
                }
            }

        };
        
        if (limit == 0) {
            child.onCompleted();
            parent.unsubscribe();
        }
        
        /*
         * We decouple the parent and child subscription so there can be multiple take() in a chain such as for
         * the groupBy Observer use case where you may take(1) on groups and take(20) on the children.
         * 
         * Thus, we only unsubscribe UPWARDS to the parent and an onComplete DOWNSTREAM.
         * 
         * However, if we receive an unsubscribe from the child we still want to propagate it upwards so we
         * register 'parent' with 'child'
         */
        child.add(parent);
        return parent;
    }

}
