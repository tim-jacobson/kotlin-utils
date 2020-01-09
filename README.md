# kotlin-utils
Set of utilities developed for working with Kotlin

## Delegates

### Custom Map Delegates
Functionality to allow the mapping of key-values of a map to the fields of a class.
This is intended to extend and improve the kotlin native delegate behavior `from <Map>`,
by allowing nullable/missing keys.

## Sequences

### Catchable Sequence
A kotlin `Sequence` can be made to become a `CatchableSequence` where by errors are captured
by item and can be handled item by item, as part of a separate step, or thrown away altogether.

Example Usage:
```kotlin
sequence("1","a","3").catchable().map{it.toInt()}.filterSuccesses() == sequence(1,3)
``` 

## TODO / IDEAS

* A utility function to conditionally update properties of a kotlin data class
