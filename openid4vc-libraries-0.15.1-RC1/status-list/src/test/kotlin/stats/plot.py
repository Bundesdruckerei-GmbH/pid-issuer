#!/bin/python
import matplotlib.pyplot as plt
import pandas as pd

df = pd.read_csv('stats.csv', sep=',')

means = df.groupby(['capacity','revoked'])['size'].mean()
print(means)
means.plot(kind='bar', title='Status lists by capacity and revoked amount', ylabel='Uncompressed size', xlabel="Group", figsize=(10,10))
plt.show()

means = df.groupby(['capacity','revoked'])['compressedSize'].mean()
print(means)
means.plot(kind='bar', title='Status lists by capacity and revoked amount', ylabel='Compressed size [bytes]', xlabel="Group", figsize=(10,11))
plt.show()